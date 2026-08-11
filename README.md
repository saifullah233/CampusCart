# CampusCart — Backend

> Everything on Campus. By Students. For Students.

Production-grade marketplace backend for CampusCart. Built with Java 21, Spring Boot 3.5,
and a feature-based, DDD-inspired modular architecture.

> **Build status:** Parts 1-8 are implemented, including the Part 8 review/report lifecycle,
> strict admin moderation, user suspension, reference-data management, audit logs, and
> admin dashboard analytics. Payment remains a deferred provider integration.
>
> **Earlier scope:** Parts 2-7 established persistence, security, authentication, OTP,
> rotating sessions, user profiles, the product marketplace, commerce, chat, WebSocket,
> notifications, and moderation foundations.

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Java 21 (bytecode target; see note below) |
| Framework | Spring Boot 3.5.16 |
| API docs | springdoc-openapi 2.9.0 (Swagger UI) |
| Build | Maven |
| Datastore | MySQL 8, Flyway |
| Security | Spring Security, signed JWT access tokens, hashed rotating refresh tokens |
| Optional cache/rate-limit infrastructure (future) | Redis 7 |
| Containerization | Docker, Docker Compose |

> **JDK note:** The build targets **Java 21** bytecode (`maven.compiler.release=21`) and
> the container image pins `eclipse-temurin:21`. The reference machine now runs **JDK
> 21.0.11 LTS**, so the earlier JDK-25 ByteBuddy workaround (Part-1 audit §4) has been
> removed. See `docs/backend-audit.md` §4 for the original context.

---

## Prerequisites

- JDK 21
- Maven 3.9+
- Docker + Docker Compose — MySQL 8 for running the app; also used by the test suite
  (Testcontainers spins up MySQL 8, so Docker must be running for `mvn verify`).

---

## Running

### Build & test

```bash
mvn clean verify
```

> Integration tests use Testcontainers, so a running Docker daemon is required.

### Run locally

Provide local-only credentials through the environment or a git-ignored `.env` file:

```powershell
$env:MYSQL_ROOT_PASSWORD = "<local-root-password>"
$env:MYSQL_PASSWORD = "<local-database-password>"
$env:DB_PASSWORD = $env:MYSQL_PASSWORD
$env:JWT_SECRET = "<random-value-at-least-32-characters>"
```

Start MySQL first (the app connects on boot and Flyway migrates the schema):

```bash
docker compose up -d mysql
mvn spring-boot:run
```

Datasource defaults use the local `campuscart` database and username; `DB_URL` and
`DB_USERNAME` remain overridable. The application requires `DB_PASSWORD` and a runtime
`JWT_SECRET`; neither has a source-controlled default.

Set `CORS_ALLOWED_ORIGINS` to the exact trusted frontend origins when needed. Never
commit the secret or a shared `.env` file.

### Verify it is up

- Liveness: `GET http://localhost:8080/api/v1/ping`
- Actuator health: `GET http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Infrastructure via Docker

Before starting Compose, provide `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`,
`DB_PASSWORD`, and `JWT_SECRET` in the shell or a git-ignored `.env` file. The
database password used by the app should match `MYSQL_PASSWORD`.

```bash
docker compose up -d mysql
```

> Compose credentials are **local development defaults only**. Override them with a
> git-ignored `.env` file; never reuse them in shared environments.
> MySQL and Redis ports are bound to `127.0.0.1` for local development. Redis is
> provisioned as optional future infrastructure only; current OTP and login abuse
> protection use the database-backed implementation in this backend.

---

## Project Layout

```
src/main/java/com/campuscart
├── CampusCartApplication.java        # entry point
├── config/                           # OpenAPI + JPA auditing config
├── common/
│   ├── api/                          # ApiResponse<T>, ApiError (response envelope)
│   ├── domain/                       # BaseEntity (UUID PK, auditing, @Version)
│   ├── exception/                    # ErrorCode, ApiException hierarchy, global handler
│   └── web/                          # HealthController (liveness)
├── security/                         # JWT, stateless filter chain, CORS, refresh tokens
├── location/domain/                  # City
├── college/domain/                   # College, CollegeEmailDomain
├── user/                             # User (domain) + UserRepository
└── catalog/domain/                   # Category

src/main/resources/db/migration       # Flyway: V1-V9, including commerce tables
```

See `docs/part2-database.md` for the schema (ER diagram, tables, constraints, indexes).

Feature modules (`auth`, `user`, `product`, `cart`, `order`, `payment`, `chat`, `admin`, …)
are introduced in controlled parts; Chat and broader Admin workflows remain deferred.

---

## Documentation

- `docs/backend-audit.md` — repository audit, environment report, and implementation status.
- `docs/security-foundation.md` — JWT, refresh-token, CORS, authorization, and secret policy.
- `docs/authentication.md` — registration, OTP lifecycle, login, sessions, and profile API.
- `docs/part2-database.md` — Part-2 schema: conventions, ER diagram, tables, constraints,
  index strategy, and migration list.
- `docs/product-marketplace.md` - product/category endpoints, discovery scopes, filters,
  ownership, and Cloudinary image policy.
- `docs/wishlist-cart-orders.md` - wishlist/cart endpoints, checkout locking, order
  transitions, authorization, and deferred payment integration.
- `docs/part8-reviews-reports-admin.md` - review eligibility, report lifecycle,
  moderation, admin APIs, authorization, audit logs, and analytics.
