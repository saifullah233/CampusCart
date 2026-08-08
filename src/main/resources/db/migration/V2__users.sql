-- V2: Users.
--
-- Core student identity only. Authentication columns (password_hash, email_verified,
-- role, status) are intentionally deferred to the auth module's own migration so the
-- schema never carries unused, unenforced columns.

CREATE TABLE users (
    id         BINARY(16)   NOT NULL,
    email      VARCHAR(255) NOT NULL,
    full_name  VARCHAR(150) NOT NULL,
    college_id BINARY(16)   NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    version    BIGINT       NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_college FOREIGN KEY (college_id) REFERENCES colleges (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_users_college_id ON users (college_id);
