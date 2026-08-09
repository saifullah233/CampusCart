# CampusCart Authentication And OTP

**Part:** 4  
**Date:** 2026-08-09

Part 4 adds the actual account, OTP, session, and profile flows. Product, Cart, and Order
modules remain out of scope.

## Student Registration

`POST /api/v1/auth/register/student` accepts `cityId`, `collegeId`, `officialEmail`,
`fullName`, and `password`.

The server verifies that:

- the college belongs to the selected city;
- the email domain is configured for that exact college;
- the email is not already registered;
- the OTP destination is below the send rate limit.

The account is created as `STUDENT` and `PENDING_VERIFICATION`. The password is stored
only as a BCrypt hash. The email OTP must be verified before the account becomes active.

## Community Registration

`POST /api/v1/auth/register/community` accepts `email`, `fullName`, `cityId`,
`phoneNumber`, and `password`.

The server validates the city and phone uniqueness, creates a `COMMUNITY` account with no
college association, and sends a phone OTP. Successful phone verification activates the
account.

## OTP Lifecycle

- `POST /api/v1/otp/verify` verifies a challenge id and numeric code.
- `POST /api/v1/otp/resend` requests a replacement code after the cooldown.
- Codes are generated with `SecureRandom`, hashed with BCrypt, and never persisted raw.
- Codes expire after five minutes by default.
- Five failed attempts invalidate a challenge.
- Resend requests are limited to five per destination within fifteen minutes, with a
  sixty-second cooldown between sends.
- A resend supersedes the previous challenge, so an older code cannot be used afterward.

The production delivery boundary is `OtpDeliveryGateway`. The default implementation
publishes an in-process `OtpDeliveryMessage` event containing the transient code for an
email/SMS adapter. It does not log or return the code. A deployment must provide the
email and SMS provider listener before enabling real outbound delivery; tests replace the
gateway with an in-memory capture without changing the application flow.

## Login And Sessions

- `POST /api/v1/auth/login` authenticates an active account with email and password.
- `POST /api/v1/auth/refresh` rotates a persisted opaque refresh token and returns a new
  access/refresh pair.
- `POST /api/v1/auth/logout` revokes the presented refresh token.
- Access tokens are short-lived signed JWTs; refresh tokens are random opaque values whose
  SHA-256 hashes are persisted.
- Reuse of a rotated refresh token revokes the user's remaining active refresh tokens.
- Suspended or pending accounts cannot log in, refresh, or access the profile API.

## User Profile

- `GET /api/v1/users/me` returns the authenticated user's profile and server-owned status,
  role, account type, city, and optional college association.
- `PATCH /api/v1/users/me` currently permits only the display name.

The controller derives identity from the verified JWT principal. No request field or path
parameter can select another user's profile or assign a role, city, college, or status.

## Configuration

OTP limits are configured under `security.otp.*` and can be overridden through the
`OTP_TTL`, `OTP_RESEND_COOLDOWN`, `OTP_RATE_WINDOW`, `OTP_MAX_ATTEMPTS`,
`OTP_MAX_SENDS_PER_WINDOW`, and `OTP_CODE_LENGTH` environment variables.

No OTP, database, SMTP, SMS, password, or JWT secret is committed to the repository.
