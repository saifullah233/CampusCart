-- V6: Registration-path fields for student and community users.

ALTER TABLE users
    ADD COLUMN phone_number  VARCHAR(32)  NULL AFTER email,
    ADD COLUMN phone_verified BIT(1)      NOT NULL DEFAULT b'0' AFTER email_verified,
    ADD COLUMN account_type  VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' AFTER status,
    ADD COLUMN city_id       BINARY(16)   NULL AFTER college_id;

-- Existing Part-2/3 users derive their city from their selected college.
UPDATE users u
JOIN colleges c ON c.id = u.college_id
SET u.city_id = c.city_id;

ALTER TABLE users
    MODIFY COLUMN city_id     BINARY(16) NOT NULL,
    MODIFY COLUMN college_id  BINARY(16) NULL,
    ADD CONSTRAINT fk_users_city FOREIGN KEY (city_id) REFERENCES cities (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE INDEX idx_users_city_id ON users (city_id);
CREATE UNIQUE INDEX uq_users_phone_number ON users (phone_number);
