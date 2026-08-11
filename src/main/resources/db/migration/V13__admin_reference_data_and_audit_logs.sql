-- V13: admin-managed reference-data activation and durable administrative audit logs.

ALTER TABLE cities
    ADD COLUMN active BIT(1) NOT NULL DEFAULT b'1' AFTER state;

ALTER TABLE colleges
    ADD COLUMN active BIT(1) NOT NULL DEFAULT b'1' AFTER city_id;

ALTER TABLE categories
    ADD COLUMN active BIT(1) NOT NULL DEFAULT b'1' AFTER slug;

CREATE INDEX idx_cities_active_name ON cities (active, name);
CREATE INDEX idx_colleges_active_city ON colleges (active, city_id);
CREATE INDEX idx_categories_active_name ON categories (active, name);

CREATE TABLE audit_logs (
    id          BINARY(16) NOT NULL,
    actor_id    BINARY(16) NOT NULL,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id   BINARY(16) NULL,
    details     VARCHAR(1000) NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    version     BIGINT NOT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_audit_logs_actor_created ON audit_logs (actor_id, created_at);
CREATE INDEX idx_audit_logs_target_created ON audit_logs (target_type, target_id, created_at);
