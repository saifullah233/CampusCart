-- V1: Geography + institutions (cities, colleges, college email domains).
--
-- Conventions (Part 2):
--   * snake_case, plural table names, InnoDB + utf8mb4.
--   * UUID primary keys stored compactly as BINARY(16).
--   * Every entity carries created_at/updated_at (DATETIME(6), UTC) + version (optimistic lock).
--   * Foreign keys are indexed and named fk_<table>_<ref>; ON DELETE/UPDATE RESTRICT
--     protects shared reference data from cascading removal.

CREATE TABLE cities (
    id         BINARY(16)   NOT NULL,
    name       VARCHAR(120) NOT NULL,
    state      VARCHAR(120) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    version    BIGINT       NOT NULL,
    CONSTRAINT pk_cities PRIMARY KEY (id),
    CONSTRAINT uq_cities_name_state UNIQUE (name, state)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE colleges (
    id         BINARY(16)   NOT NULL,
    name       VARCHAR(200) NOT NULL,
    city_id    BINARY(16)   NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    version    BIGINT       NOT NULL,
    CONSTRAINT pk_colleges PRIMARY KEY (id),
    CONSTRAINT uq_colleges_name_city UNIQUE (name, city_id),
    CONSTRAINT fk_colleges_city FOREIGN KEY (city_id) REFERENCES cities (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Dedicated index for the FK: the composite unique above leads with `name`, so it
-- cannot serve lookups/joins on city_id alone.
CREATE INDEX idx_colleges_city_id ON colleges (city_id);

CREATE TABLE college_email_domains (
    id         BINARY(16)   NOT NULL,
    domain     VARCHAR(255) NOT NULL,
    college_id BINARY(16)   NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    version    BIGINT       NOT NULL,
    CONSTRAINT pk_college_email_domains PRIMARY KEY (id),
    CONSTRAINT uq_college_email_domains_domain UNIQUE (domain),
    CONSTRAINT fk_college_email_domains_college FOREIGN KEY (college_id) REFERENCES colleges (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_college_email_domains_college_id ON college_email_domains (college_id);
