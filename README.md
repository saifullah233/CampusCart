# CampusCart — Backend

> Everything on Campus. By Students. For Students.

Production-grade marketplace backend for CampusCart, built with **Java 21**, **Spring Boot
3.5**, and a feature-based, DDD-inspired modular architecture.

> **Status:** Backend feature set complete and production-hardened. Registration/OTP,
> JWT + rotating refresh sessions, user profiles, the product marketplace with discovery
> scopes, wishlist/cart/transactional checkout, orders, chat + WebSocket, notifications,
> reviews, reports, admin moderation, audit logs, and dashboard analytics are all
> implemented and covered by an integration test suite running against real MySQL.
>
> Two boundaries require deployment configuration for full production use: **OTP email/SMS
> delivery** and **Cloudinary image storage**. Payment is a deferred provider integration
> that never fabricates success. See the [final audit](docs/final-backend-audit.md).

---

## Tech stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 21 (Eclipse Temurin) |
| Framework | Spring Boot 3.5.16 (Web, Security, Data JPA, WebSocket, Actuator, Validation) |
| Build | Maven → fat jar `campuscart-backend.jar` |
| Datastore | MySQL 8, Flyway (V1–V15) |
| Security | Spring Security, HS256 JWT access tokens, SHA-256 hashed rotating refresh tokens, BCrypt |
| Realtime | STOMP over WebSocket |
| API docs | springdoc-openapi 2.9 (Swagger UI) |
| Image storage | Cloudinary (optional, off by default) |
| Containerization | Docker (multi-stage, non-root, healthcheck), Docker Compose |
| CI | GitHub Actions (build, test, package, image build) |

---

## Quickstart

Prerequisites: **JDK 21**, **Maven 3.9+**, **Docker** (for MySQL and the test suite).

```bash
# 1) Run the tests (Testcontainers starts MySQL 8; no env vars needed)
mvn clean verify

# 2) Run from source
cp .env.example .env                 # then edit DB_PASSWORD (= MYSQL_PASSWORD) and JWT_SECRET
docker compose up -d mysql           # start MySQL; Flyway migrates on app boot
mvn spring-boot:run

# 3) Or run the whole stack in containers
docker compose up -d --build
```

Verify:

- Liveness — `GET http://localhost:8080/api/v1/ping`
- Health — `GET http://localhost:8080/actuator/health`
- Swagger UI — `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON — `http://localhost:8080/v3/api-docs`

`JWT_SECRET` (≥ 32 chars) and `DB_PASSWORD` are **required with no default** — the app and
Compose fail fast without them. Full variable reference:
[docs/deployment/README.md](docs/deployment/README.md#2-environment-variables).

---

## Documentation

| Area | Document |
|---|---|
| **Architecture** — modules, layering, request flow, WebSocket topology, integration ports | [docs/architecture/README.md](docs/architecture/README.md) |
| **API** — envelope, endpoint catalog, error codes, WebSocket destinations | [docs/api/README.md](docs/api/README.md) |
| **Database** — schema, tables, indexes, Flyway migrations | [docs/database/README.md](docs/database/README.md) |
| **Security** — auth, authorization, boundaries, secrets, CORS/headers | [docs/security/README.md](docs/security/README.md) |
| **Deployment** — env, local setup, Docker, Redis, Cloudinary, email, CI, scaling | [docs/deployment/README.md](docs/deployment/README.md) |
| **Final audit** — production-readiness classification | [docs/final-backend-audit.md](docs/final-backend-audit.md) |

Feature deep-dives: [authentication](docs/authentication.md) ·
[security foundation](docs/security-foundation.md) ·
[product marketplace](docs/product-marketplace.md) ·
[wishlist/cart/orders](docs/wishlist-cart-orders.md) ·
[reviews/reports/admin](docs/part8-reviews-reports-admin.md) ·
[part 2 schema](docs/part2-database.md).

---

## Project layout

```
src/main/java/com/campuscart
├── CampusCartApplication.java        # entry point
├── config/                           # OpenAPI, JPA auditing
├── common/                           # ApiResponse/ApiError/PageResponse, BaseEntity, exceptions, utils
├── security/                         # JWT filter chain, CORS, refresh tokens, OTP, login throttle, WebSocket auth
├── auth/                             # registration, OTP, login, refresh, logout
├── user/ location/ college/ catalog/ # accounts + reference data (city, college, category)
├── product/                          # products, images, likes, discovery
├── cart/ wishlist/                   # user-scoped cart & wishlist
├── order/ payment/                   # transactional checkout, order lifecycle, payment boundary
├── chat/                             # conversations, messages, blocks, reports, moderation, WebSocket
├── review/ notification/             # reviews + moderation, in-app + push notifications
└── admin/ audit/                     # dashboard/analytics, audit log

src/main/resources/db/migration       # Flyway V1–V15
.github/workflows/ci.yml              # CI: build, test, package, docker build
Dockerfile · docker-compose.yml · .env.example
```

---

## Testing

`mvn clean verify` compiles, runs the full test suite against a real MySQL 8 (Testcontainers,
applying all Flyway migrations), and packages the jar. A running Docker daemon is required.

---

## License

Proprietary. © CampusCart.
