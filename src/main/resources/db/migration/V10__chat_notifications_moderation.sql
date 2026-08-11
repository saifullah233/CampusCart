-- V10: chat, moderation, product likes, and durable notifications.

CREATE TABLE conversations (
    id               BINARY(16) NOT NULL,
    buyer_id         BINARY(16) NOT NULL,
    seller_id        BINARY(16) NOT NULL,
    product_id       BINARY(16) NOT NULL,
    last_message_at  DATETIME(6) NULL,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    version          BIGINT NOT NULL,
    CONSTRAINT pk_conversations PRIMARY KEY (id),
    CONSTRAINT uq_conversations_participants_product UNIQUE (buyer_id, seller_id, product_id),
    CONSTRAINT fk_conversations_buyer FOREIGN KEY (buyer_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_conversations_seller FOREIGN KEY (seller_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_conversations_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_conversations_buyer_updated ON conversations (buyer_id, updated_at);
CREATE INDEX idx_conversations_seller_updated ON conversations (seller_id, updated_at);
CREATE INDEX idx_conversations_product ON conversations (product_id);

CREATE TABLE chat_messages (
    id                    BINARY(16) NOT NULL,
    conversation_id       BINARY(16) NOT NULL,
    sender_id             BINARY(16) NOT NULL,
    message_type          VARCHAR(20) NOT NULL,
    content               TEXT NULL,
    image_storage_key     VARCHAR(512) NULL,
    image_delivery_url    VARCHAR(1024) NULL,
    image_content_type    VARCHAR(80) NULL,
    image_size_bytes      BIGINT NULL,
    shared_product_id     BINARY(16) NULL,
    moderation_status     VARCHAR(30) NOT NULL,
    read_at               DATETIME(6) NULL,
    created_at            DATETIME(6) NOT NULL,
    updated_at            DATETIME(6) NOT NULL,
    version               BIGINT NOT NULL,
    CONSTRAINT pk_chat_messages PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_chat_messages_product FOREIGN KEY (shared_product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_chat_messages_conversation_created ON chat_messages (conversation_id, created_at);
CREATE INDEX idx_chat_messages_unread ON chat_messages (conversation_id, sender_id, read_at);
CREATE INDEX idx_chat_messages_report_product ON chat_messages (shared_product_id);

CREATE TABLE user_blocks (
    id          BINARY(16) NOT NULL,
    blocker_id  BINARY(16) NOT NULL,
    blocked_id  BINARY(16) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    version     BIGINT NOT NULL,
    CONSTRAINT pk_user_blocks PRIMARY KEY (id),
    CONSTRAINT uq_user_blocks_pair UNIQUE (blocker_id, blocked_id),
    CONSTRAINT ck_user_blocks_not_self CHECK (blocker_id <> blocked_id),
    CONSTRAINT fk_user_blocks_blocker FOREIGN KEY (blocker_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_user_blocks_blocked FOREIGN KEY (blocked_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_user_blocks_blocked ON user_blocks (blocked_id);

CREATE TABLE chat_reports (
    id               BINARY(16) NOT NULL,
    reporter_id      BINARY(16) NOT NULL,
    conversation_id  BINARY(16) NOT NULL,
    message_id       BINARY(16) NULL,
    reason           VARCHAR(80) NOT NULL,
    details          VARCHAR(1000) NULL,
    status           VARCHAR(20) NOT NULL,
    reviewed_by      BINARY(16) NULL,
    reviewed_at      DATETIME(6) NULL,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    version          BIGINT NOT NULL,
    CONSTRAINT pk_chat_reports PRIMARY KEY (id),
    CONSTRAINT fk_chat_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_chat_reports_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_chat_reports_message FOREIGN KEY (message_id) REFERENCES chat_messages (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_chat_reports_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_chat_reports_status_created ON chat_reports (status, created_at);
CREATE INDEX idx_chat_reports_conversation ON chat_reports (conversation_id, created_at);

CREATE TABLE product_likes (
    id          BINARY(16) NOT NULL,
    user_id     BINARY(16) NOT NULL,
    product_id  BINARY(16) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    version     BIGINT NOT NULL,
    CONSTRAINT pk_product_likes PRIMARY KEY (id),
    CONSTRAINT uq_product_likes_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_product_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_product_likes_product FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_product_likes_product_created ON product_likes (product_id, created_at);

CREATE TABLE notifications (
    id          BINARY(16) NOT NULL,
    user_id     BINARY(16) NOT NULL,
    type        VARCHAR(40) NOT NULL,
    title       VARCHAR(180) NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    data_json   JSON NULL,
    read_at     DATETIME(6) NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    version     BIGINT NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at);
CREATE INDEX idx_notifications_unread ON notifications (user_id, read_at, created_at);
