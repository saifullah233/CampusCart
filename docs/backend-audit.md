# CampusCart Backend Audit

**Date:** 2026-08-09
**Auditor:** Codex
**Scope:** Complete repository audit of the existing backend implementation.
**Rule of engagement:** No production-code changes were made during the initial audit.

**Follow-up:** Parts 3-5 continued from this audited state after the audit was completed.
The current implementation status is recorded in the follow-up sections below.

## Executive Summary

CampusCart is currently a small Spring Boot backend with a solid Part 1/Part 2
foundation: Maven project setup, Docker Compose infrastructure, OpenAPI metadata,
standard API/error envelopes, JPA auditing, Flyway migrations, and reference identity
entities for cities, colleges, college email domains, users, and categories.

The repository had uncommitted work that appeared to be a partial Part 3 security/auth
foundation. At the time of the initial audit it was not complete enough to run: the
application context was **BROKEN** because `JwtAuthenticationFilter` required
`JwtService`, and `JwtService` required an unregistered `JwtProperties` bean.

That initial finding was addressed in the Part 3 follow-up. Part 4 then added the
student and community registration, OTP verification, login, profile, and session
flows. Part 5 then added the product/category marketplace and Cloudinary image boundary.
The current application starts, applies nine migrations, and passes the full
authentication/security/marketplace/commerce test suite.

No separate "CampusCart master specification" file was found in the repository. This
audit compares the actual implementation against the in-project specification baseline:
`README.md`, `docs/part2-database.md`, the previous audit history, and the documented
future modules (`auth`, `product`, `cart`, `order`, `payment`, `chat`, `admin`, Redis
OTP, email, Cloudinary/media, WebSocket notifications).

## Git State And History

Status: **IMPLEMENTED for current scope / DIRTY WORKTREE**

- Latest commits:
  - `cd7d714 feat(database): implement Part 2 persistence foundation`
  - `502f41d Part 1: repository audit, environment verification, and minimal runnable bootstrap`
- Modified tracked files:
  - `pom.xml`
  - `src/main/java/com/campuscart/common/exception/ErrorCode.java`
  - `src/main/java/com/campuscart/common/exception/GlobalExceptionHandler.java`
  - `src/main/java/com/campuscart/user/domain/User.java`
- Untracked files:
  - `.qwen/`
  - `src/main/java/com/campuscart/common/api/PageResponse.java`
  - `src/main/java/com/campuscart/common/exception/InvalidTokenException.java`
  - `src/main/java/com/campuscart/common/util/`
  - `src/main/java/com/campuscart/security/`
  - `src/main/java/com/campuscart/user/domain/AccountStatus.java`
  - `src/main/java/com/campuscart/user/domain/Role.java`
  - `src/main/resources/db/migration/V4__user_auth_columns.sql`

Interpretation: Part 1 and Part 2 are committed. The security/auth foundation is
present as uncommitted work and must be handled carefully.

## Verification

Initial audit status: **BROKEN**. Current follow-up status: **PASSING**.

Command run:

```text
mvn test
```

Current result after the Part 6 follow-up:

- `mvn -DskipTests compile` passed.
- `mvn -DskipTests test-compile` passed.
- `mvn test` passed with 39 tests and 0 failures/errors.
- Testcontainers started MySQL 8 successfully.
- Flyway validated and applied nine migrations (`V1` through `V9`).
- Hibernate schema validation and the full Spring context passed.
- Security tests cover JWT expiry/tampering, public/protected routes, JSON 401/403
  envelopes, CORS, method/URL role authorization, and refresh-token rotation/replay.

## Build And Dependencies

Status: **IMPLEMENTED for current auth/marketplace scope; PARTIALLY IMPLEMENTED for future modules**

Implemented:

- Maven project with Spring Boot `3.5.16`.
- Java release target `21`.
- Web, validation, actuator, springdoc OpenAPI.
- JPA, MySQL Connector/J, Flyway core + MySQL support.
- Cloudinary HTTP client, enabled only through explicit deployment configuration.
- Testcontainers MySQL and Spring Boot test support.

Partially implemented:

- Spring Security, JJWT, Cloudinary storage support, and focused auth/marketplace tests
  are present.

Needs attention:

- Chat and broader admin workflows remain future scope; payment-provider connection is
  intentionally deferred behind the implemented payment boundary.

## Runtime Configuration

Status: **IMPLEMENTED for current auth/marketplace scope; PARTIALLY IMPLEMENTED for future integrations**

Implemented:

- `application.yml` configures datasource, JPA `ddl-auto=validate`, Hibernate UTC,
  Flyway migrations, actuator health/info exposure, springdoc paths, and safe server
  error settings.
- Docker profile points datasource at the `mysql` Compose service.

Needs attention:

- Cloudinary is disabled by default and requires deployment credentials before image
  uploads are enabled.
- Redis, email/SMS, and WebSocket integrations remain future infrastructure.

## Docker And Local Infrastructure

Status: **IMPLEMENTED for foundation, PARTIALLY IMPLEMENTED for future modules**

Implemented:

- Multi-stage Dockerfile using Maven on Temurin 21 and runtime Temurin 21 JRE.
- Non-root runtime user.
- Compose services for MySQL 8, Redis 7, and app.
- MySQL and Redis health checks.
- Local-development credential defaults are called out as non-production values.

Needs attention:

- Compose starts Redis for future modules; current OTP and marketplace state use MySQL
  and Cloudinary is configured through environment variables when enabled.

## Database Migrations

Status: **IMPLEMENTED through Part 5**

Implemented:

- `V1__geo_and_institutions.sql`: `cities`, `colleges`, `college_email_domains`, FK
  indexes, uniqueness constraints.
- `V2__users.sql`: core `users` table with unique email and college FK.
- `V3__categories.sql`: flat `categories` table with unique name and slug.
- `V4__user_auth_columns.sql`: additive auth columns on `users`:
  `password_hash`, `email_verified`, `role`, `status`, plus role/status indexes.
- `V5__refresh_tokens.sql`: hashed rotating refresh-token sessions.
- `V6__user_registration_fields.sql`: city, phone, account type, and optional college.
- `V7__otp_challenges.sql`: hashed OTP challenges and abuse-control state.
- `V8__products_and_images.sql`: product, image, foreign-key, and marketplace indexes.
- `V9__wishlist_cart_orders_payments.sql`: wishlist, cart, order, order-item snapshot,
  payment tables, uniqueness constraints, foreign keys, and commerce indexes.

Good signs:

- Migrations are additive and preserve existing Part 2 schema.
- Flyway successfully validates and applies all nine migrations in tests.
- Hibernate validates all current entity mappings at startup.

Needs attention:

- Enum columns intentionally use application-owned validation for MySQL portability;
  product lifecycle and discovery transitions are enforced in the service/domain layer.

## Domain Entities

Status: **IMPLEMENTED for current identity/auth/marketplace scope**

Implemented:

- `BaseEntity`: UUID identity, `created_at`, `updated_at`, optimistic `version`,
  Hibernate-safe equality.
- `City`: name/state uniqueness.
- `College`: many-to-one city relation, unique name per city.
- `CollegeEmailDomain`: globally unique domain mapped to college.
- `Category`: flat taxonomy with unique name and slug.
- `User`: unique email, full name, college relation.

Implemented:

- `User` includes server-controlled auth and registration fields, including account type,
  city, optional college, phone, role, status, and verification state.
- `Role`, `AccountStatus`, and `UserType` enums are present.
- `Product` and `ProductImage` include seller, geography, category, independent product
  type/reach fields, lifecycle state, timestamps, and optimistic versioning.
- `Category` remains a flat reference taxonomy. Wishlist, cart, order, order-item, and
  payment entities are now present; chat remains future scope.

## Repositories

Status: **IMPLEMENTED for auth/location/marketplace scope**

Implemented:

- Auth/location/college repositories, `CategoryRepository`, `ProductRepository`,
  `ProductImageRepository`, `OtpChallengeRepository`, and `RefreshTokenRepository`.
- Product search uses `JpaSpecificationExecutor` with server-side discovery predicates.
- Chat/admin workflow repositories remain future scope; wishlist/cart/order/payment
  repositories are implemented.

## DTOs And API Contracts

Status: **IMPLEMENTED for current API scope**

Implemented:

- `ApiResponse<T>` common success/error envelope.
- `ApiError` with machine-readable code, detail, path, and field errors.
- `PageResponse<T>` wrapper for stable paged responses.
- `HealthController.PingResponse` record.

Implemented:

- Auth, OTP, profile, category, product, image, search, pagination, and common response DTOs.
- MockMvc API tests cover the current auth/category/product contracts.
- Chat/admin DTOs remain future scope; wishlist/cart/order/payment contracts are present.

## Controllers

Status: **IMPLEMENTED for current auth/category/product scope**

Implemented:

- `GET /api/v1/ping` liveness endpoint.
- Actuator health/info endpoints configured through Spring Boot.
- Auth, OTP, user/profile, category, product, and product-image controllers.
- URL/method authorization and standard JSON security errors.

Future scope:

- College lookup, chat, notification, and broader admin controllers.

## Services

Status: **IMPLEMENTED for current auth/security/marketplace scope**

Implemented:

- JWT, password, refresh-token, OTP, auth, user, category, product, image, and search
  services with transaction boundaries and server-owned identity/ownership.
- Chat/admin workflows remain future scope; wishlist/cart/order/payment services are present.

## Authentication And Security

Status: **IMPLEMENTED**

Implemented:

- `Role` enum with Spring Security authority strings.
- `AccountStatus` enum with `canAuthenticate`.
- `AuthenticatedUser` principal implements `UserDetails`.
- `JwtAuthenticationFilter` extracts bearer tokens and populates the security context.
- `JwtService` signs/verifies JWTs with issuer, subject, email, role, issued-at, and
  expiration claims.
- Auth-related error codes and exception handling paths exist.

Implemented:

- Registered JWT/OTP/Cloudinary properties, UTC clock, BCrypt, stateless filter chain,
  CORS, method security, JWT authentication, 401/403 handlers, and role authorization.
- Auth, refresh rotation/logout, OTP, product ownership, and server-side discovery checks
  are backed by persisted state and covered by tests.

Risk:

- Future protected modules must continue to use the same active-account and ownership
  policy instead of trusting client identifiers.

## Exception Handling

Status: **IMPLEMENTED for current scope**

Implemented:

- `ApiException` hierarchy.
- Validation, malformed JSON, method-not-supported, data integrity, authentication,
  access denied, and unexpected exception handlers.
- Responses avoid stack traces and internal DB details.

Needs attention:

- `UNSUPPORTED_MEDIA_TYPE` exists in `ErrorCode` but there is no override for
  unsupported media type yet.
- Filter-chain security failures use the dedicated JSON entry point/access-denied handler.

## Tests

Status: **IMPLEMENTED**

Implemented:

- Context-load test against real MySQL via Testcontainers.
- Schema/table checks.
- Persistence round-trip for city -> college -> domain -> user.
- Category persistence/auditing check.

Current result:

- Full `mvn test` passes with 39 tests and zero failures/errors.
- Coverage includes MySQL schema validation, auth/OTP, JWT/refresh, role access,
  category authorization, product types/reaches, visibility, ownership, filters,
  pagination/sorting, and image validation.

## Documentation

Status: **IMPLEMENTED for current scope; future module docs remain pending**

Implemented:

- README documents project purpose, stack, running instructions, Docker infra, layout,
  product-marketplace, and current Part 6 commerce status.
- `docs/part2-database.md` thoroughly documents Part 2 schema and verification.
- `docs/security-foundation.md` documents reusable security policy and token handling.
- `docs/authentication.md` documents registration, OTP, login, refresh, logout, and
  profile APIs, including the outbound delivery boundary.
- This audit records the current Part 6 classifications.

## Part 3 Follow-up

Implemented after the initial audit as the reusable security foundation:

- Registered and validated JWT/CORS properties, a UTC clock, BCrypt encoder, stateless
  `SecurityFilterChain`, method security, explicit public routes, and secure CORS.
- Added uniform JSON `AuthenticationEntryPoint` and `AccessDeniedHandler` responses.
- Hardened JWT parsing so malformed claims and parser failures produce one generic error
  and no token/parser details are logged.
- Added V5 `refresh_tokens` persistence, hashed opaque tokens, expiry, row-locked rotation,
  replacement lineage, and replay revocation.
- Added database user-details lookup for future password authentication without exposing a
  login endpoint or allowing Spring Boot to create a generated default user.
- Added focused unit/integration tests and documented the policy in
  `docs/security-foundation.md`.

Part 3 deliberately deferred the actual registration and OTP flows. Part 4 now implements
those flows. Marketplace modules remain deliberately deferred.

## Part 4 Follow-up

Current Part 4 classification:

| Area | Status | Finding |
|---|---|---|
| Student registration | IMPLEMENTED | City, city-scoped college, normalized official email, exact configured college-domain validation, password hashing, and pending account creation. |
| Community registration | IMPLEMENTED | Normalized email/phone, city association, password hashing, and pending account creation. |
| OTP generation/storage | IMPLEMENTED | Secure numeric codes are BCrypt-hashed and persisted in `otp_challenges`; raw codes are not persisted or logged. |
| OTP verification | IMPLEMENTED | Email is used for students and phone for communities; successful verification activates the correct account. |
| OTP expiry/attempts/cooldown/rate limiting | IMPLEMENTED | Configurable expiry, maximum attempts, resend cooldown, and destination-window send limits are enforced under row locks. |
| OTP delivery | PARTIALLY IMPLEMENTED | `OtpDeliveryGateway` is the provider boundary; the default emits an in-process event, so deployment still needs an SMTP/SMS adapter. |
| Login/password authentication | IMPLEMENTED | BCrypt password verification, active-account checks, and JWT/refresh issuance. |
| JWT access tokens | IMPLEMENTED | Signed, issuer-bound, expiring access tokens with persisted user role claims. |
| Refresh tokens | IMPLEMENTED | Opaque hashed persistence, expiry, row-locked rotation, replay revocation, and logout invalidation. |
| User profile/status | IMPLEMENTED | Authenticated profile read/update, server-controlled status, and suspended-account rejection. |
| College association/ownership | IMPLEMENTED | College must belong to the selected city and its configured email domain; request DTOs cannot set role or ownership. |
| Common API/error infrastructure | IMPLEMENTED | Standard success/error envelopes, validation errors, error codes, global handler, pagination wrapper, and JSON security failures. |
| Role authorization | IMPLEMENTED | URL and method security use persisted server-side roles. |
| Part 4 tests | IMPLEMENTED | 26 tests pass, including registration, OTP abuse cases, JWT, refresh rotation, role access, and suspension. |
| Product marketplace | IMPLEMENTED | Product/category CRUD, lifecycle, server-side discovery, filters, pagination, sorting, ownership, and image validation are present. |
| Cart/order | IMPLEMENTED | Wishlist/cart APIs, transactional checkout, stock reservation, order snapshots, and status management are present. |

## Part 5 Follow-up

Current Part 5 classification:

| Area | Status | Finding |
|---|---|---|
| Product types | IMPLEMENTED | `NEW` and `SECOND_HAND` are separate enum values and persisted fields. |
| Selling reach | IMPLEMENTED | `MY_CAMPUS`, `OTHER_COLLEGES`, and `PUBLIC` are separate from product condition. |
| Product ownership | IMPLEMENTED | Seller is derived from the JWT-backed user; seller/admin checks happen in the service layer. |
| Product geography/category | IMPLEMENTED | City, optional college, and category are server-assigned or validated from persisted records. |
| Product lifecycle | IMPLEMENTED | Create, update, soft delete, sold, activate, and deactivate operations are present. |
| Marketplace discovery | IMPLEMENTED | Scope-aware server predicates enforce college, city, community, and public reach. |
| Search/filter/pagination/sorting | IMPLEMENTED | Keyword, category, type, reach, college, city, price, status, allow-listed sort, and bounded pages. |
| Category API | IMPLEMENTED | Authenticated reads plus admin-only create/update/delete, with product-reference deletion protection. |
| Product image API | IMPLEMENTED FOUNDATION | Ownership/count/type/size/magic-byte checks and Cloudinary authenticated-storage boundary are present. |
| Product indexes | IMPLEMENTED | V8 adds status/reach/geography, category, type, price, seller, and image indexes. |
| Part 5 tests | IMPLEMENTED | Product type/reach, visibility, ownership, unauthorized mutation/deletion, query filters, category role access, and image validation are covered. |
| Cart/order | IMPLEMENTED | Part 6 adds wishlist/cart APIs, transactional checkout, order snapshots, and lifecycle authorization. |

## Part 6 Follow-up

Current Part 6 classification:

| Area | Status | Finding |
|---|---|---|
| Wishlist add/remove/list/check | IMPLEMENTED | Authenticated user scope, server-side product discovery, application duplicate check, and database uniqueness constraint are present. |
| Cart management | IMPLEMENTED | Add, remove, quantity replacement, totals, stale-line availability, and checkout readiness are present. |
| Cart quantity/availability safety | IMPLEMENTED | Positive quantities, active status, stock checks, own-product protection, product row locks, and cart-line locks are enforced. |
| Order creation | IMPLEMENTED | One transaction reserves cart products, writes buyer/seller item snapshots, creates a payment record, and clears the cart. |
| Order access | IMPLEMENTED | Buyer, involved seller, and admin checks are enforced server-side; unrelated users receive access denied. |
| Seller order management | IMPLEMENTED | Involved sellers can accept, reject, ship, or mark delivery through the status graph. |
| Order lifecycle | IMPLEMENTED | `PLACED`, `ACCEPTED`, `REJECTED`, `SHIPPED`, `DELIVERED`, `COMPLETED`, and `CANCELLED` transitions are domain-validated. |
| Cancellation/completion | IMPLEMENTED | Buyer shortcuts and status endpoints are available; cancellation/rejection restores reserved quantity. |
| Payment persistence | IMPLEMENTED | Each order has one payment record, initially `NOT_CONNECTED`, with provider fields kept separate from order state. |
| Payment integration | IMPLEMENTED FOUNDATION | `PaymentGateway` is injectable, but the default provider returns a safe 503; no fake payment success is returned. |
| Part 6 indexes | IMPLEMENTED | V9 adds user/product, order buyer/status, seller/order-item, and payment indexes plus uniqueness constraints. |
| Part 6 tests | IMPLEMENTED | Four MySQL API integration tests cover wishlist, cart availability, order authorization/transitions, cancellation restocking, and deferred payment. |
| Part 6 documentation | IMPLEMENTED | `docs/wishlist-cart-orders.md` documents endpoints, locks, status transitions, and deferred payment behavior. |

## Feature Classification

| Feature / Area | Status | Notes |
|---|---|---|
| Git repository/history | IMPLEMENTED | Two baseline commits exist; current Part 3/4/5/6 work remains uncommitted in the dirty worktree. |
| Maven/Spring Boot setup | IMPLEMENTED | Compiles with Java 21 after dependencies resolve. |
| Common API envelope | IMPLEMENTED | `ApiResponse`, `ApiError`; `PageResponse` added but untracked. |
| Global exception handling | IMPLEMENTED | MVC and filter-chain failures use the standard envelope without stack traces. |
| OpenAPI metadata | IMPLEMENTED | Includes the JWT bearer security scheme. |
| Health/liveness endpoint | IMPLEMENTED | `GET /api/v1/ping` is explicitly public. |
| Dockerfile | IMPLEMENTED | Multi-stage, Java 21, non-root runtime. |
| Docker Compose MySQL/Redis/app | IMPLEMENTED FOUNDATION | App requires environment-driven database and JWT secrets; no secret fallback is supplied. |
| JPA auditing/base entity | IMPLEMENTED | UUID, timestamps, optimistic locking. |
| Flyway migrations V1-V3 | IMPLEMENTED | Matches Part 2 persistence foundation. |
| Flyway migration V4 | IMPLEMENTED | User auth columns apply and are validated with Hibernate. |
| Flyway migration V5 | IMPLEMENTED | Hashed refresh-token persistence with expiry/revocation lineage. |
| Flyway migration V6 | IMPLEMENTED | City, phone, account-type, and optional-college registration fields. |
| Flyway migration V7 | IMPLEMENTED | OTP challenge persistence with hashed codes and abuse controls. |
| City/College/CollegeEmailDomain entities | IMPLEMENTED | Mappings match migrations. |
| Category entity | IMPLEMENTED | Flat taxonomy only, as documented. |
| User core identity | IMPLEMENTED | Email/full name/college mapping present. |
| User auth and registration fields | IMPLEMENTED | Entity/migrations cover account type, city, phone, verification state, role, and status. |
| Repositories | IMPLEMENTED | Auth/location/college/OTP/refresh/category/product/image, wishlist, cart, order, order-item, and payment repositories are present. |
| DTOs | IMPLEMENTED | Common, auth, OTP, user-profile, category, product, search, image, wishlist, cart, order, and payment contracts are present. |
| Services | IMPLEMENTED | Auth, OTP, user, JWT, refresh, category, product, image, wishlist, cart, order, and payment services are present. |
| Spring Security wiring | IMPLEMENTED | Stateless filter chain, method security, CORS, 401/403 handlers. |
| JWT authentication | IMPLEMENTED | Signed access-token issuance/parsing, expiry, tamper rejection, and login are tested. |
| Password authentication | IMPLEMENTED | BCrypt registration/login and active-account checks are implemented. |
| OTP generation/verification | IMPLEMENTED | Email/phone channels, expiry, attempts, cooldown, rate limiting, and verification are implemented. |
| OTP outbound delivery | PARTIALLY IMPLEMENTED | Provider gateway/event boundary exists; concrete SMTP/SMS adapter is deployment-specific. |
| Refresh tokens/sessions | IMPLEMENTED | Hashed persistence, expiry, row-locked rotation, replay revocation, and logout endpoint. |
| User/profile API | IMPLEMENTED | Authenticated profile read/update and status enforcement are present. |
| College/category lookup API | PARTIALLY IMPLEMENTED | Category CRUD/read API exists; separate college lookup API remains out of scope. |
| Product/listing marketplace | IMPLEMENTED | Product domain, V8 migration, lifecycle APIs, discovery/search, ownership, and tests exist. |
| Media/Cloudinary | IMPLEMENTED FOUNDATION | Cloudinary boundary, authenticated storage settings, validation, ownership, and limits exist; credentials are deployment-supplied. |
| Cart | IMPLEMENTED | User-scoped CRUD, availability/quantity validation, row locks, and checkout protection. |
| Orders | IMPLEMENTED | Transactional checkout, snapshots, authorization, state transitions, cancellation, completion, and stock restoration. |
| Payments | IMPLEMENTED FOUNDATION | Persistence and provider boundary are present; provider integration remains deferred and never reports fake success. |
| Chat/WebSocket | MISSING | No config/dependency/domain/API. |
| Admin | MISSING | Role enum exists; no admin workflows. |
| Tests | IMPLEMENTED | The 39-test suite covers auth/security, marketplace, wishlist, cart availability, order authorization/transitions, cancellation restocking, and deferred payment behavior. |
| README/docs | IMPLEMENTED | README, audit, security-foundation, authentication, product-marketplace, and commerce docs reflect the current scope. |

## Initial Audit Recommendation (Completed)

The single safest next implementation task identified by the initial audit was:

**Finish the minimal Spring Security foundation so the existing app starts again, without
adding user-facing auth flows yet.**

Scope should be deliberately small:

- Register `JwtProperties`.
- Add a production `Clock` bean.
- Add `security.jwt.*` configuration placeholders and Docker `JWT_SECRET` wiring.
- Add a `SecurityFilterChain` with stateless sessions, CSRF disabled for the API,
  `JwtAuthenticationFilter` registered, uniform JSON 401/403 handlers, and an explicit
  public allow-list for `/api/v1/ping`, `/actuator/health`, `/v3/api-docs/**`, and
  `/swagger-ui/**`.
- Add focused tests proving the context loads, public endpoints remain public, protected
  endpoints return the standard envelope, and JWT parsing works with a fixed clock.

This recommendation is now complete. The current stable base preserves the existing
persistence functionality and is ready for the next separately scoped feature task.
