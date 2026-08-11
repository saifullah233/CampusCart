# CampusCart Backend — Architecture

> Everything on Campus. By Students. For Students.

A modular, feature-based Spring Boot backend for a student-to-student campus marketplace.
This document describes the runtime shape of the system, how a request flows through it,
the module layout, and the integration boundaries that make external services optional.

---

## 1. Principles

- **Stateless HTTP.** No server-side sessions. Every request authenticates from a signed
  JWT access token in the `Authorization` header; authorization/ownership derive from the
  verified token, never from request bodies or path parameters.
- **Feature-based modular layout.** Code is organised by domain module
  (`auth`, `user`, `product`, `cart`, `order`, `payment`, `chat`, `review`, `notification`,
  `admin`, `catalog`, `location`, `college`, `audit`, `security`, `common`), each with its
  own `domain` / `repository` / `service` / `web` / `dto` layers.
- **Server owns identity and state.** Role, account status, seller, city/college, prices,
  and lifecycle transitions are assigned/validated server-side. Request DTOs never bind to
  entities directly and cannot set privileged fields.
- **Schema is authoritative and code-verified.** Flyway owns the schema; Hibernate runs in
  `validate` mode so the mappings can never silently drift from the migrations.
- **External services are optional behind interfaces.** Image storage, OTP delivery, and
  payment are ports with safe default adapters, so the app boots and runs without any
  third-party credentials.

---

## 2. Tech stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 21 (`maven.compiler.release=21`), Eclipse Temurin |
| Framework | Spring Boot 3.5.16 (MVC, Security, Data JPA, WebSocket, Actuator, Validation) |
| Build | Maven (`spring-boot-maven-plugin`, fat jar `campuscart-backend.jar`) |
| Persistence | MySQL 8, Hibernate ORM 6, Flyway migrations |
| Auth | Spring Security, JJWT 0.12 (HS256 access tokens), BCrypt, hashed rotating refresh tokens |
| Realtime | STOMP over WebSocket (`spring-boot-starter-websocket`) |
| API docs | springdoc-openapi 2.9 (Swagger UI) |
| Image storage | Cloudinary (`cloudinary-http44`), enabled only when configured |
| Testing | JUnit 5, Spring Boot Test, Testcontainers MySQL 8, spring-security-test |
| Packaging | Multi-stage Docker image (non-root, healthcheck), Docker Compose |

---

## 3. Module map

```
com.campuscart
├── CampusCartApplication            # @SpringBootApplication entry point
├── config/                          # OpenApiConfig, JpaAuditingConfig
├── common/
│   ├── api/                         # ApiResponse<T>, ApiError, PageResponse<T>
│   ├── domain/                      # BaseEntity (UUID PK, auditing, @Version)
│   ├── exception/                   # ErrorCode, ApiException hierarchy, GlobalExceptionHandler
│   ├── util/                        # Hashing (SHA-256), SecureRandomTokens, ContactNormalizer
│   └── web/                         # HealthController (liveness)
├── security/
│   ├── SecurityConfig               # stateless filter chain, CORS, method security
│   ├── JwtService / JwtAuthenticationFilter / AuthenticatedUser
│   ├── ApiAuthenticationEntryPoint / ApiAccessDeniedHandler / SecurityErrorResponseWriter
│   ├── refresh/                     # RefreshToken(Service|Repository) — hashed rotation
│   ├── otp/                         # OtpChallenge, OtpDeliveryGateway (port) + event adapter
│   ├── login/                       # LoginRateLimit(Service|Repository) — DB throttle
│   └── websocket/                   # WebSocketConfig, JwtStompChannelInterceptor
├── auth/                            # registration, OTP, login, refresh, logout (AuthService, OtpService)
├── user/                            # User domain, profile API, AdminUserController
├── location/ · college/ · catalog/ # reference data (City, College, CollegeEmailDomain, Category)
├── product/                        # Product, ProductImage, ProductLike, discovery, image storage port
├── cart/ · wishlist/               # user-scoped cart & wishlist
├── order/ · payment/               # transactional checkout, order lifecycle, payment gateway port
├── chat/                           # conversations, messages, blocks, reports, moderation, WS controller
├── review/                         # completed-transaction reviews + moderation
├── notification/                   # in-app notifications + WS push
├── admin/ · audit/                 # dashboard/analytics, audit log
└── resources/db/migration/         # Flyway V1–V15
```

See [../database/README.md](../database/README.md) for the schema and
[../api/README.md](../api/README.md) for the endpoint catalog.

---

## 4. Layering and request flow

A typical authenticated REST request:

```
HTTP request
  → CorsFilter (configured origins)
  → JwtAuthenticationFilter        # parse+verify token, re-check persisted account status,
  │                                # set SecurityContext principal (AuthenticatedUser)
  → authorizeHttpRequests          # URL rules: public / authenticated / ROLE_ADMIN
  → @RestController (web/)          # @Valid DTO in, @AuthenticationPrincipal for identity
  → @Service (service/)            # @Transactional boundary; business rules; ownership checks
  → Repository (repository/)       # Spring Data JPA / Specifications; row locks where needed
  → Entity (domain/)               # server-owned state transitions (no public role/status setters)
  → Mapper → DTO → ApiResponse<T>  # uniform success envelope
```

Cross-cutting concerns:

- **Response envelope** — every controller returns `ApiResponse<T>` (success) or, on failure,
  `ApiResponse` wrapping an `ApiError` (machine code, human detail, request path, field
  violations). Paginated reads return `PageResponse<T>`.
- **Exception handling** — `GlobalExceptionHandler` (`@RestControllerAdvice`) maps the
  `ApiException` hierarchy and framework exceptions to the envelope. 4xx are logged at WARN
  (message only); 5xx are logged at ERROR with a stack trace **server-side only** — response
  bodies never contain stack traces, SQL, or internal detail. `server.error.include-*` are
  all disabled.
- **Filter-chain failures** — bypass `@RestControllerAdvice`, so `ApiAuthenticationEntryPoint`
  (401) and `ApiAccessDeniedHandler` (403) emit the same envelope via `SecurityErrorResponseWriter`.
- **Auditing** — `JpaAuditingConfig` + `BaseEntity` stamp `created_at`/`updated_at` and manage
  the optimistic-lock `@Version` for every entity (UUID primary keys).

---

## 5. Persistence approach

- **Flyway owns the schema** (`V1`–`V15`), applied automatically on boot; `ddl-auto=validate`
  guarantees the JPA mappings match. Timestamps are stored/read in UTC.
- **All associations are `FetchType.LAZY`** and there are no ORM collection mappings; child
  collections are always loaded through repositories. This avoids accidental eager joins.
- **N+1 defence** — list endpoints bulk-load: product search and order/wishlist/conversation
  listings fetch associations and aggregates in batched queries (join fetch / `@EntityGraph` /
  `IN`-clause maps) rather than per row. A global
  `hibernate.default_batch_fetch_size=50` collapses any residual lazy initialisation into
  batched `IN` loads.
- **Concurrency** — checkout, refresh-token rotation, OTP verification, and stock
  reservation use `PESSIMISTIC_WRITE` row locks; products additionally carry optimistic
  versions.

---

## 6. Realtime / WebSocket topology

- STOMP over a single raw-WebSocket endpoint `/ws`, with allowed origins restricted to the
  configured CORS origins (no wildcard).
- `JwtStompChannelInterceptor` authenticates the `CONNECT` frame from the JWT and authorises
  `SUBSCRIBE` frames (e.g. a conversation topic is only subscribable by its participants).
- Broker: Spring's **in-memory `SimpleBroker`** for `/topic` and `/queue`; application
  destination prefix `/app`; user destination prefix `/user`.
  - Chat messages publish to `/topic/conversations/{id}`; per-user notifications publish to
    `/user/queue/notifications`.
- **Scaling note:** the in-memory broker and per-session subscription/auth state are
  node-local. The service is correct and secure on a single instance; running multiple
  instances requires an external STOMP broker relay (RabbitMQ/ActiveMQ) plus sticky sessions.
  See [../deployment/README.md](../deployment/README.md#scaling--websocket).

---

## 7. Integration boundaries (ports with safe defaults)

| Port (interface) | Default adapter (no config) | Real adapter |
|---|---|---|
| `ProductImageStorage` / `ChatImageStorage` | `Unavailable…Storage` → safe `503`-style error | `Cloudinary…Storage` when `cloudinary.enabled=true` + credentials |
| `OtpDeliveryGateway` | `ApplicationEventOtpDeliveryGateway` → publishes an in-process event, never logs the code | a deployment `@Component`/`@EventListener` sending email/SMS |
| `PaymentGateway` | `UnavailablePaymentGateway` → `PAYMENT_INTEGRATION_UNAVAILABLE` | a provider-confirmed adapter |

Because each boundary has a safe default, the application starts and every non-integration
feature works without any third-party credentials. Enabling a real feature is purely a
configuration/adapter concern — documented in [../deployment/README.md](../deployment/README.md).

---

## 8. Key decisions

- **JWT + opaque refresh** — short-lived signed access tokens (stateless auth) paired with
  high-entropy opaque refresh tokens stored only as SHA-256 digests, with row-locked rotation
  and replay revocation. See [../security/README.md](../security/README.md).
- **DB-backed abuse control** — OTP challenges and login rate limits are persisted (hashed),
  so no Redis/cache is required for correctness.
- **Redis is not used** by the current backend; it is optional infrastructure only.
- **Everything testable against real MySQL** — integration tests run on Testcontainers MySQL 8
  and apply the production migrations, so tests exercise the shipped schema, not an H2 stand-in.
