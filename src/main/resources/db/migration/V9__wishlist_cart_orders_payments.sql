-- V9: Wishlist, cart, orders, order-item snapshots, and deferred payment foundation.

CREATE TABLE wishlist_items (
    id         BINARY(16)  NOT NULL,
    user_id    BINARY(16)  NOT NULL,
    product_id BINARY(16)  NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version    BIGINT      NOT NULL,
    CONSTRAINT pk_wishlist_items PRIMARY KEY (id),
    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_wishlist_items_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_wishlist_items_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_wishlist_items_user_created ON wishlist_items (user_id, created_at);
CREATE INDEX idx_wishlist_items_product ON wishlist_items (product_id);

CREATE TABLE cart_items (
    id         BINARY(16) NOT NULL,
    user_id    BINARY(16) NOT NULL,
    product_id BINARY(16) NOT NULL,
    quantity   INT        NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version    BIGINT     NOT NULL,
    CONSTRAINT pk_cart_items PRIMARY KEY (id),
    CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_cart_items_user_created ON cart_items (user_id, created_at);
CREATE INDEX idx_cart_items_product ON cart_items (product_id);

CREATE TABLE orders (
    id           BINARY(16)    NOT NULL,
    buyer_id     BINARY(16)    NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    version      BIGINT        NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_orders_buyer_created ON orders (buyer_id, created_at);
CREATE INDEX idx_orders_status_created ON orders (status, created_at);

CREATE TABLE order_items (
    id             BINARY(16)    NOT NULL,
    order_id       BINARY(16)    NOT NULL,
    product_id     BINARY(16)    NOT NULL,
    seller_id      BINARY(16)    NOT NULL,
    product_title  VARCHAR(180)  NOT NULL,
    unit_price     DECIMAL(12,2) NOT NULL,
    quantity       INT           NOT NULL,
    line_total     DECIMAL(12,2) NOT NULL,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    version        BIGINT        NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT uq_order_items_order_product UNIQUE (order_id, product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_order_items_seller FOREIGN KEY (seller_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_seller_created ON order_items (seller_id, created_at);
CREATE INDEX idx_order_items_product ON order_items (product_id);

CREATE TABLE payments (
    id                  BINARY(16)    NOT NULL,
    order_id            BINARY(16)    NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    status              VARCHAR(30)   NOT NULL,
    provider            VARCHAR(80)   NULL,
    provider_payment_id VARCHAR(180)  NULL,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    version             BIGINT        NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_payments_status_created ON payments (status, created_at);
