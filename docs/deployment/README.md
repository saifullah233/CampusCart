# CampusCart Backend — Deployment

Local setup, environment configuration, database, Docker, testing, and production
deployment. Commands assume the repository root.

---

## 1. Prerequisites

- **JDK 21** (Eclipse Temurin recommended)
- **Maven 3.9+** (or use the in-repo build via Docker)
- **Docker + Docker Compose** — required to run MySQL and to run the test suite
  (Testcontainers starts a real MySQL 8).

---

## 2. Environment variables

Copy the template and fill in real values (the file is git-ignored):

```bash
cp .env.example .env
```

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `MYSQL_ROOT_PASSWORD` | ✅ (compose) | — | MySQL container root password |
| `MYSQL_DATABASE` | | `campuscart` | App database name |
| `MYSQL_USER` | | `campuscart` | App database user |
| `MYSQL_PASSWORD` | ✅ (compose) | — | App database user password |
| `DB_PASSWORD` | ✅ (app) | — | App datasource password (**must equal** `MYSQL_PASSWORD`) |
| `DB_URL` | | local: `jdbc:mysql://localhost:3306/campuscart`; docker profile: `…//mysql:3306/…` | Datasource URL override |
| `DB_USERNAME` | | `campuscart` | Datasource user override |
| `JWT_SECRET` | ✅ (app) | — | HS256 signing secret, **≥ 32 chars** (`openssl rand -base64 48`) |
| `JWT_ISSUER` | | `campuscart` | Token issuer |
| `JWT_ACCESS_TOKEN_TTL` | | `15m` | Access token lifetime |
| `JWT_REFRESH_TOKEN_TTL` | | `14d` | Refresh token lifetime |
| `CORS_ALLOWED_ORIGINS` | | `http://localhost:3000,http://localhost:5173` | Exact frontend origins (no wildcards) |
| `CLOUDINARY_ENABLED` | | `false` | Enable image storage |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | | empty | Cloudinary credentials |
| `OTP_*` | | see `.env.example` | OTP TTL / attempts / cooldown / rate window / length |
| `LOGIN_MAX_FAILURES` / `LOGIN_RATE_WINDOW` / `LOGIN_LOCKOUT` | | `5` / `15m` / `15m` | Login throttle policy |
| `SPRING_PROFILES_ACTIVE` | | (unset locally; `docker` in Compose) | Active profile |

Required variables have **no source-controlled default** — the app and Compose fail fast
if they are missing.

---

## 3. Local setup (run from source)

```bash
# 1. Start MySQL only
docker compose up -d mysql

# 2. Export the app secrets (PowerShell shown; use export on bash)
#    $env:DB_PASSWORD = "<matches MYSQL_PASSWORD>"
#    $env:JWT_SECRET  = "<random >= 32 chars>"

# 3. Run the app (Flyway migrates on boot)
mvn spring-boot:run
```

Verify it is up:

- Liveness: `GET http://localhost:8080/api/v1/ping`
- Health: `GET http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## 4. Database & Flyway

- **MySQL 8** is the only datastore. Schema is owned by **Flyway** (`V1`–`V15` in
  `src/main/resources/db/migration`) and applied automatically on application boot.
- Hibernate runs in `validate` mode — startup fails if the entities do not match the schema.
- No manual migration step is needed: `docker compose up -d mysql` then start the app, and
  Flyway brings the schema up to date. Tests apply the same migrations via Testcontainers.
- See [../database/README.md](../database/README.md) for the table/index reference.

---

## 5. Redis

Redis is **not used** by the current backend — OTP challenges and login throttling are
database-backed. It is provisioned in Compose behind an opt-in profile for future modules:

```bash
docker compose --profile optional up -d   # includes redis
```

The default `docker compose up` starts only MySQL + app.

---

## 6. Cloudinary (image storage)

Optional. With `CLOUDINARY_ENABLED=false` the app runs but product/chat image upload returns
a safe configuration error. To enable:

```bash
CLOUDINARY_ENABLED=true
CLOUDINARY_CLOUD_NAME=<cloud>
CLOUDINARY_API_KEY=<key>
CLOUDINARY_API_SECRET=<secret>
```

Images are stored in a server-owned folder with authenticated delivery.

---

## 7. Email / OTP delivery

The OTP module generates, hashes, and verifies codes correctly, but **outbound delivery is a
deployment boundary**. The default `OtpDeliveryGateway` publishes an in-process
`OtpDeliveryMessage` event (it never logs the code) and does **not** send email/SMS.

For real user onboarding, provide an adapter — either a Spring bean implementing
`OtpDeliveryGateway`, or an `@EventListener(OtpDeliveryMessage.class)` — wired to your email
(SMTP/provider) and SMS provider. No `spring-boot-starter-mail` is bundled, so add the
transport dependency your provider needs. Until then, registration cannot complete for real
users. This is tracked as **NEEDS CONFIGURATION**.

---

## 8. Docker

Build the production image (multi-stage, non-root, healthcheck):

```bash
docker build -t campuscart-backend:local .
```

Run the full stack with Compose (build + MySQL + app):

```bash
# with a populated .env
docker compose up -d --build
docker compose logs -f app
docker compose ps          # app shows healthy once /actuator/health is UP
docker compose down        # add -v to also drop the mysql volume
```

The image runs as a non-root user, sets a container-aware JVM
(`-XX:MaxRAMPercentage=75.0`), and exposes `8080` with a `HEALTHCHECK` on
`/actuator/health`.

---

## 9. Testing

```bash
mvn clean verify     # compile + all tests (Testcontainers MySQL) + package the jar
```

A running Docker daemon is required (Testcontainers). Tests supply their own datasource and
JWT secret via `@DynamicPropertySource`, so no environment variables are needed for tests.
Test coverage is summarised in [../final-backend-audit.md](../final-backend-audit.md).

---

## 10. CI/CD

`.github/workflows/ci.yml` runs on push/PR to `main`:

1. **build-test** — JDK 21, `mvn -B -ntp clean verify` (compile + tests + package), verifies
   and uploads `target/campuscart-backend.jar`, uploads Surefire reports.
2. **docker-build** — verifies the production image builds end-to-end (no push).

GitHub-hosted `ubuntu-latest` runners include Docker, so Testcontainers and the image build
work without extra setup.

---

## 11. Production deployment

Recommended shape:

1. **Provision MySQL 8** (managed instance or container) and create the database/user.
2. **Set environment**: `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, a strong `JWT_SECRET`, exact
   `CORS_ALLOWED_ORIGINS`, and (if used) Cloudinary credentials + `CLOUDINARY_ENABLED=true`.
3. **Terminate TLS** at the load balancer/ingress (enables HSTS); route to app port `8080`.
4. **Deploy the image** (`campuscart-backend:<tag>`); Flyway migrates on boot.
5. **Wire OTP delivery** (email/SMS adapter) — required for registration.
6. **Health/monitoring**: liveness `GET /api/v1/ping`, readiness/health
   `GET /actuator/health`; scrape logs (JSON envelope, no secrets).

### Scaling & WebSocket

The app is stateless for HTTP and scales horizontally behind a load balancer **for REST**.
The chat/notification WebSocket uses an **in-memory STOMP broker** whose subscription and
per-session auth state are node-local. To run **more than one instance** with realtime
messaging you must:

- introduce an external STOMP broker relay (RabbitMQ/ActiveMQ) via
  `enableStompBrokerRelay(...)`, and
- enable **sticky sessions** for the WebSocket handshake at the load balancer.

Single-instance deployment needs neither and is fully functional. Tracked as a
**KNOWN LIMITATION**.

---

## 12. Exact run commands (summary)

```bash
# Tests (needs Docker)
mvn clean verify

# Run from source (needs MySQL + DB_PASSWORD + JWT_SECRET)
docker compose up -d mysql
mvn spring-boot:run

# Full container stack (needs .env)
docker compose up -d --build

# Build image only
docker build -t campuscart-backend:local .
```
