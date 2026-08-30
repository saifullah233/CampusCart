-- V21: Single-use, short-lived password reset authorizations.
-- Stores SHA-256 token_hash of cryptographically random reset tokens.

CREATE TABLE password_reset_authorizations (
    id             BINARY(16)   NOT NULL,
    user_id        BINARY(16)   NOT NULL,
    challenge_id   BINARY(16)   NOT NULL,
    token_hash     VARCHAR(64)  NOT NULL,
    expires_at     DATETIME(6)  NOT NULL,
    used_at        DATETIME(6)  NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    version        BIGINT       NOT NULL,
    CONSTRAINT pk_password_reset_authorizations PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_password_reset_challenge FOREIGN KEY (challenge_id) REFERENCES otp_challenges (id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_password_reset_user_id ON password_reset_authorizations (user_id);
CREATE INDEX idx_password_reset_expires_at ON password_reset_authorizations (expires_at);
