CREATE TABLE login_rate_limits (
    id BINARY(16) NOT NULL,
    identity_hash VARCHAR(64) NOT NULL,
    window_started_at DATETIME(6) NOT NULL,
    failure_count INT NOT NULL,
    locked_until DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT pk_login_rate_limits PRIMARY KEY (id),
    CONSTRAINT ux_login_rate_limits_identity UNIQUE (identity_hash)
);

CREATE INDEX idx_login_rate_limits_locked_until ON login_rate_limits (locked_until);
