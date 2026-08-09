-- V8: Marketplace products and securely stored product images.
--
-- product_type and selling_reach are independent dimensions. Do not collapse them
-- into a single visibility enum: product condition and discovery audience are separate.

CREATE TABLE products (
    id            BINARY(16)    NOT NULL,
    seller_id     BINARY(16)    NOT NULL,
    college_id    BINARY(16)    NULL,
    city_id       BINARY(16)    NOT NULL,
    category_id   BINARY(16)    NOT NULL,
    title         VARCHAR(180)  NOT NULL,
    description   TEXT          NOT NULL,
    price         DECIMAL(12,2) NOT NULL,
    product_type  VARCHAR(20)   NOT NULL,
    selling_reach VARCHAR(30)   NOT NULL,
    quantity      INT           NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    version       BIGINT        NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_products_college FOREIGN KEY (college_id) REFERENCES colleges (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_products_city FOREIGN KEY (city_id) REFERENCES cities (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_products_status_reach_city_created
    ON products (status, selling_reach, city_id, created_at);
CREATE INDEX idx_products_status_reach_college_created
    ON products (status, selling_reach, college_id, created_at);
CREATE INDEX idx_products_category_status_created
    ON products (category_id, status, created_at);
CREATE INDEX idx_products_type_status_created
    ON products (product_type, status, created_at);
CREATE INDEX idx_products_price_status_created
    ON products (price, status, created_at);
CREATE INDEX idx_products_seller_status_created
    ON products (seller_id, status, created_at);

CREATE TABLE product_images (
    id           BINARY(16)   NOT NULL,
    product_id   BINARY(16)   NOT NULL,
    storage_key  VARCHAR(512) NOT NULL,
    delivery_url VARCHAR(1024) NOT NULL,
    content_type VARCHAR(80)  NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    version      BIGINT       NOT NULL,
    CONSTRAINT pk_product_images PRIMARY KEY (id),
    CONSTRAINT uq_product_images_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_product_images_product_created
    ON product_images (product_id, created_at);
