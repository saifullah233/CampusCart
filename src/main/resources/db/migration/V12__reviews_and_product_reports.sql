-- V12: reviews and product targets in the existing report model.

ALTER TABLE chat_reports
    DROP CHECK ck_chat_reports_target;

ALTER TABLE chat_reports
    ADD COLUMN reported_product_id BINARY(16) NULL AFTER reported_user_id;

ALTER TABLE chat_reports
    ADD CONSTRAINT fk_chat_reports_reported_product FOREIGN KEY (reported_product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE chat_reports
    ADD CONSTRAINT ck_chat_reports_target CHECK (
        conversation_id IS NOT NULL OR reported_user_id IS NOT NULL OR reported_product_id IS NOT NULL);

CREATE INDEX idx_chat_reports_reported_product ON chat_reports (reported_product_id, created_at);

CREATE TABLE reviews (
    id                BINARY(16) NOT NULL,
    reviewer_id       BINARY(16) NOT NULL,
    reviewed_user_id  BINARY(16) NOT NULL,
    product_id        BINARY(16) NOT NULL,
    order_id          BINARY(16) NOT NULL,
    rating            INT NOT NULL,
    review_text       VARCHAR(2000) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    moderated_by     BINARY(16) NULL,
    moderated_at     DATETIME(6) NULL,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    version           BIGINT NOT NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT uq_reviews_reviewer_order_product UNIQUE (reviewer_id, order_id, product_id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reviews_reviewed_user FOREIGN KEY (reviewed_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reviews_moderator FOREIGN KEY (moderated_by) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_reviews_product_status_created ON reviews (product_id, status, created_at);
CREATE INDEX idx_reviews_reviewed_user_status ON reviews (reviewed_user_id, status);
CREATE INDEX idx_reviews_order ON reviews (order_id);
