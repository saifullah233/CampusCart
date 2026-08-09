-- V5: Persisted opaque refresh tokens for rotation and revocation.
-- Raw refresh tokens are never stored; the application stores only token_hash.

CREATE TABLE refresh_tokens (
    id             BINARY(16)   NOT NULL,
    user_id        BINARY(16)   NOT NULL,
    token_hash     VARCHAR(64)  NOT NULL,
    expires_at     DATETIME(6)  NOT NULL,
    revoked_at     DATETIME(6)  NULL,
    replaced_by_id BINARY(16)   NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    version        BIGINT       NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
