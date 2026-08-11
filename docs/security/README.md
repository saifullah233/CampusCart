# CampusCart Backend — Security

This document describes the security posture: authentication, authorization, the trust
boundaries, and the input/secret handling policies. It reflects what is enforced in code.

---

## 1. Posture summary

- Stateless bearer-token API; no HTTP sessions; CSRF disabled because auth is a header,
  not an ambient cookie.
- All identity and authority come from a **signed** access token verified on every request;
  request bodies/path params are never trusted for identity or ownership.
- Only `ACTIVE` accounts can authenticate or act; suspension takes effect immediately.
- Secrets are environment-only with no source-controlled fallback; error responses never
  leak internals.

---

## 2. Authentication

### Access tokens (`JwtService`)
- HS256 signed JWT: claims `iss`, `sub` (user UUID), `email`, `role`, `iat`, `exp`.
- Short-lived (`JWT_ACCESS_TOKEN_TTL`, default **15m**). Signing key from `JWT_SECRET`
  (**≥ 32 chars**, enforced at construction — the app fails fast on a weak/missing key).
- Verification checks signature + issuer + expiry against an injected UTC clock. Every
  failure collapses to one generic `InvalidTokenException`; token material and parser detail
  are never logged.
- `JwtAuthenticationFilter` re-loads the persisted user and **re-checks account status on
  every request**, so a still-valid token from a now-suspended/pending account is rejected
  (403 `ACCOUNT_NOT_ACTIVE`).

### Refresh tokens (`RefreshTokenService`)
- Opaque high-entropy random values; only a **SHA-256 digest** is persisted (`refresh_tokens`).
- Rotation is transactional and row-locked (`PESSIMISTIC_WRITE`): the presented token is
  validated, revoked, and replaced; replacement lineage is recorded.
- **Replay of a revoked token revokes all of that user's active refresh tokens** — a
  conservative response that neutralises a stolen token chain.
- Account status is re-checked on issue and rotate. Lifetime `JWT_REFRESH_TOKEN_TTL`
  (default **14d**).

### Password login (`AuthService`)
- BCrypt (`strength 12`) password verification; only active accounts receive tokens.
- Failed-login throttling is DB-backed (`login_rate_limits`, storing only a SHA-256 hash of
  the normalized email key): default **5 failures / 15 min → 15 min lockout**
  (`LOGIN_MAX_FAILURES`, `LOGIN_RATE_WINDOW`, `LOGIN_LOCKOUT`). No user-enumeration signal:
  unknown email and wrong password behave identically.

### OTP (`OtpService`) — registration verification
- Numeric codes from `SecureRandom`, **BCrypt-hashed**, never persisted or logged in the clear.
- Row-locked verification. Enforced: expiry (`OTP_TTL`, default 5m), max attempts
  (`OTP_MAX_ATTEMPTS`, default 5), resend cooldown (`OTP_RESEND_COOLDOWN`, default 60s), and a
  per-destination send window (`OTP_MAX_SENDS_PER_WINDOW` in `OTP_RATE_WINDOW`). A resend
  supersedes the prior challenge.
- **Delivery is a boundary** (`OtpDeliveryGateway`); the default adapter publishes an
  in-process event only — see [§9](#9-configuration-required-for-real-use).

---

## 3. Authorization

- **Roles:** `STUDENT`, `COMMUNITY`, `ADMIN` (server-assigned; no public setter, no DTO binding).
- **URL rules** (`SecurityConfig`): public allow-list (`/api/v1/ping`, `/actuator/health`,
  `/v3/api-docs/**`, `/swagger-ui/**`, `/ws`, `/api/v1/auth/**`, `/api/v1/otp/**`, `/error`,
  `OPTIONS`); `/api/v1/admin/**` requires `ROLE_ADMIN`; everything else requires
  authentication.
- **Method security** (`@EnableMethodSecurity`) plus service-layer checks; admin services also
  re-validate the persisted admin account.
- **Ownership & resource rules** are enforced in services from the verified principal:
  - Product mutate/delete/lifecycle → admin **or** the seller (`requireWritableProduct`).
  - Order view/transition → admin, buyer, or an involved seller; buyers may only
    cancel/complete, sellers only accept/reject/ship/deliver.
  - Cart/wishlist/chat/report/review actions are scoped to the authenticated user; you cannot
    buy your own product, review without a completed order, or report your own content.
- **Discovery / marketplace reach** — a hidden or out-of-reach product returns **404, not 403**,
  so IDs cannot be probed. Reach rules: `MY_CAMPUS` (same college), `OTHER_COLLEGES`
  (same city), `PUBLIC` (everywhere); community sellers cannot use `MY_CAMPUS`.

---

## 4. Security boundaries

| Boundary | Control |
|---|---|
| HTTP → app | JWT verification + per-request account-status re-check |
| Public vs authenticated vs admin | URL rules + method security + service re-validation |
| Owner-only actions | Service checks against verified principal id; 404 for undiscoverable resources |
| WebSocket CONNECT/SUBSCRIBE | JWT auth on CONNECT; participant check on SUBSCRIBE |
| Uploaded files | Type allowlist + magic bytes + structural parse + size + pixel cap |
| Outbound errors | Uniform envelope; no stack traces / SQL / internals |
| Secrets | Environment-only; fail-fast; nothing committed |

---

## 5. Input & upload safety

- Bean Validation on all request DTOs; malformed JSON and bad path/query types normalise to
  `MALFORMED_REQUEST` (not a 500/stack trace).
- Image uploads (`ImageFileValidator`): allowlist JPEG/PNG/WEBP, magic-byte signature check,
  full `ImageIO` structural parse, **5 MB** size cap, and a **20 MP** decompression-bomb cap.
  Multipart limits are set in `application.yml`.
- Chat images additionally pass a safety-scan hook and are stored with a moderation status.

---

## 6. CORS & headers

- CORS origins are **explicit and configured** (`CORS_ALLOWED_ORIGINS`); wildcard origins are
  rejected at startup; credentials are disabled (bearer header, not cookies).
- Security headers rely on Spring Security's secure servlet defaults
  (`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, no-store cache control on
  security responses). HSTS applies when served over HTTPS (terminate TLS at the ingress).

---

## 7. Secrets & configuration

- `JWT_SECRET` and `DB_PASSWORD` are **required with no default**; the app and Docker Compose
  fail fast when they are absent. Cloudinary/OTP/JWT-issuer/CORS are env-driven.
- `.gitignore` excludes `.env*` and `application-local*.yml`; `.env.example` ships
  placeholders only. No secret exists in `application.yml`, the Dockerfile, or source.

---

## 8. Logging & data protection

- Logging defaults to `INFO`; SQL logging is off. Tokens, OTP codes, passwords, and refresh
  values are never logged. Rate-limit/OTP lookup keys are stored only as SHA-256 hashes.
- 4xx logged as WARN (code + method + path); 5xx logged as ERROR with a server-side stack
  trace; response bodies stay generic.

---

## 9. Configuration required for real use

These are safe-by-default boundaries, not defects, but must be configured for a full
production deployment:

- **OTP delivery (email/SMS)** — `NEEDS CONFIGURATION`. The default gateway does not send
  codes to users; provide an `OtpDeliveryGateway` implementation (or an `@EventListener` for
  `OtpDeliveryMessage`) wired to your email/SMS provider before real user onboarding.
- **Cloudinary** — `NEEDS CONFIGURATION` for image upload; otherwise upload returns a safe
  configuration error.
- **HTTPS/HSTS** — terminate TLS at the load balancer/ingress; set exact
  `CORS_ALLOWED_ORIGINS`.

## 10. Known limitation

- **Multi-instance WebSocket** — the in-memory STOMP broker is single-instance; horizontal
  scale-out needs an external broker relay + sticky sessions. Single-instance operation is
  fully functional and secure. See [../deployment/README.md](../deployment/README.md#scaling--websocket).
