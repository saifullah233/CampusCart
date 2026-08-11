-- V11: extend the existing chat report model to support standalone user reports.

ALTER TABLE chat_reports
    MODIFY conversation_id BINARY(16) NULL;

ALTER TABLE chat_reports
    ADD COLUMN reported_user_id BINARY(16) NULL AFTER conversation_id;

ALTER TABLE chat_reports
    ADD CONSTRAINT fk_chat_reports_reported_user FOREIGN KEY (reported_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE chat_reports
    ADD CONSTRAINT ck_chat_reports_target CHECK (conversation_id IS NOT NULL OR reported_user_id IS NOT NULL);

CREATE INDEX idx_chat_reports_reported_user ON chat_reports (reported_user_id, created_at);
