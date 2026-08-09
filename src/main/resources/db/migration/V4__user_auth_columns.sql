-- V4: Authentication columns on users (Part 3 security foundation).
--
-- Additive-only change to the Part-2 `users` table. Adds the auth dimension that was
-- deliberately deferred:
--   * password_hash  — adaptive (BCrypt) hash; NULLABLE until the OTP registration flow
--                       sets it. Never stores plaintext or a reversible value.
--   * email_verified — proof-of-ownership flag; defaults false.
--   * role           — server-assigned authorization role; defaults STUDENT so a row can
--                       never come into existence with elevated privileges.
--   * status         — account lifecycle; defaults PENDING_VERIFICATION.
--
-- role/status are stored as VARCHAR (EnumType.STRING) rather than native ENUM so the
-- application owns the value set and adding a role/status later needs no ALTER TABLE.

ALTER TABLE users
    ADD COLUMN password_hash  VARCHAR(100) NULL              AFTER full_name,
    ADD COLUMN email_verified BIT(1)       NOT NULL DEFAULT b'0' AFTER password_hash,
    ADD COLUMN role           VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' AFTER email_verified,
    ADD COLUMN status         VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION' AFTER role;

-- Common auth read paths filter by role (admin listings) and status (active-account
-- checks); index them so those scans stay cheap as the table grows.
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_status ON users (status);
