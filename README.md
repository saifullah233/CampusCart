# CampusCart — Backend

> Everything on Campus. By Students. For Students.

Production-grade marketplace backend for CampusCart. Built with Java 21, Spring Boot 3.5,
and a feature-based, DDD-inspired modular architecture.

> **Build status:** Part 2 (persistence foundation: JPA + Flyway schema for users,
> colleges, cities, college email domains, categories). Remaining marketplace domains
> (product, cart, order, payment, chat, admin, …) are **not** implemented yet and are
> delivered in later parts.

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Java 21 (bytecode target; see note below) |
| Framework | Spring Boot 3.5.16 |
| API docs | springdoc-openapi 2.9.0 (Swagger UI) |
| Build | Maven |
| Datastore | MySQL 8, Flyway |
| Cache/OTP (later parts) | Redis 7 |
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

Start MySQL first (the app connects on boot and Flyway migrates the schema):

```bash
docker compose up -d mysql
mvn spring-boot:run
```

Datasource defaults (`jdbc:mysql://localhost:3306/campuscart`, user/pass `campuscart`)
match the compose service and are overridable via `DB_URL` / `DB_USERNAME` /
`DB_PASSWORD`.

### Verify it is up

- Liveness: `GET http://localhost:8080/api/v1/ping`
- Actuator health: `GET http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Infrastructure (MySQL + Redis) via Docker

```bash
docker compose up -d mysql redis
```

> Compose credentials are **local development defaults only**. Override them with a
> git-ignored `.env` file; never reuse them in shared environments.

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
├── location/domain/                  # City
├── college/domain/                   # College, CollegeEmailDomain
├── user/                             # User (domain) + UserRepository
└── catalog/domain/                   # Category

src/main/resources/db/migration       # Flyway: V1 geo/institutions, V2 users, V3 categories
```

See `docs/part2-database.md` for the schema (ER diagram, tables, constraints, indexes).

Feature modules (`auth`, `user`, `product`, `cart`, `order`, `payment`, `chat`, `admin`, …)
are introduced in subsequent, controlled parts.

---

## Documentation

- `docs/backend-audit.md` — repository audit, environment report, and Part-1 change log.
- `docs/part2-database.md` — Part-2 schema: conventions, ER diagram, tables, constraints,
  index strategy, and migration list.
