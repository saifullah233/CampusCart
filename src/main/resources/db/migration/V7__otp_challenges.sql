-- V7: Hashed, expiring OTP challenges with attempt and resend state.

CREATE TABLE otp_challenges (
    id               BINARY(16)   NOT NULL,
    user_id          BINARY(16)   NOT NULL,
    channel          VARCHAR(10)  NOT NULL,
    purpose          VARCHAR(20)  NOT NULL,
    destination_hash VARCHAR(64)  NOT NULL,
    code_hash        VARCHAR(100) NOT NULL,
    expires_at       DATETIME(6)  NOT NULL,
    next_resend_at   DATETIME(6)  NOT NULL,
    attempt_count    INT          NOT NULL DEFAULT 0,
    verified_at      DATETIME(6)  NULL,
    superseded_at    DATETIME(6)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    version          BIGINT       NOT NULL,
    CONSTRAINT pk_otp_challenges PRIMARY KEY (id),
    CONSTRAINT fk_otp_challenges_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_otp_challenges_user_id ON otp_challenges (user_id);
CREATE INDEX idx_otp_challenges_destination_created
    ON otp_challenges (destination_hash, created_at);
