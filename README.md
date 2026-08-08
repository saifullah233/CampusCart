# CampusCart — Backend

> Everything on Campus. By Students. For Students.

Production-grade marketplace backend for CampusCart. Built with Java 21, Spring Boot 3.5,
and a feature-based, DDD-inspired modular architecture.

> **Build status:** Part 1 (foundation) only. Marketplace domains (product, cart, order,
> payment, chat, admin, …) are **not** implemented yet and are delivered in later parts.

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Java 21 (bytecode target; see note below) |
| Framework | Spring Boot 3.5.16 |
| API docs | springdoc-openapi 2.9.0 (Swagger UI) |
| Build | Maven |
| Datastore (later parts) | MySQL 8, Flyway |
| Cache/OTP (later parts) | Redis 7 |
| Containerization | Docker, Docker Compose |

> **JDK note:** The reference machine has **JDK 25** installed while the spec targets
> **Java 21**. The build cross-compiles to Java 21 bytecode (`maven.compiler.release=21`)
> and the container image pins `eclipse-temurin:21`. Installing **Temurin 21** locally is
> recommended for exact parity. See `docs/backend-audit.md` §4.

---

## Prerequisites

- JDK 21 (recommended) — a newer JDK can build via release-21 cross-compilation.
- Maven 3.9+
- Docker + Docker Compose (for MySQL/Redis in later parts)

---

## Running

### Build & test

```bash
mvn clean verify
```

### Run locally

```bash
mvn spring-boot:run
```

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

## Project Layout (Part 1)

```
src/main/java/com/campuscart
├── CampusCartApplication.java        # entry point
├── config/                           # OpenAPI (Swagger) config
└── common/
    ├── api/                          # ApiResponse<T>, ApiError (response envelope)
    ├── exception/                    # ErrorCode, ApiException hierarchy, global handler
    └── web/                          # HealthController (liveness)
```

Feature modules (`auth`, `user`, `product`, `cart`, `order`, `payment`, `chat`, `admin`, …)
are introduced in subsequent, controlled parts.

---

## Documentation

- `docs/backend-audit.md` — repository audit, environment report, and Part-1 change log.
