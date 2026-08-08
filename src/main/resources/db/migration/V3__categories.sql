-- V3: Product categories (flat taxonomy).
--
-- Standalone reference data for Part 2. Product tables and their product_type /
-- selling_reach dimensions arrive in a later part and are out of scope here.

CREATE TABLE categories (
    id         BINARY(16)   NOT NULL,
    name       VARCHAR(120) NOT NULL,
    slug       VARCHAR(140) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    version    BIGINT       NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
