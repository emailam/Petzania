-- USERS TABLE
CREATE TABLE users
(
    user_id  UUID PRIMARY KEY,
    username VARCHAR(32)  NOT NULL UNIQUE,
    email    VARCHAR(100) NOT NULL UNIQUE
);

CREATE INDEX idx_user_id ON users (user_id);

-- ADMINS TABLE
CREATE TABLE admins
(
    admin_id UUID PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    role     VARCHAR(32) NOT NULL
);

-- NOTIFICATIONS TABLE
CREATE TABLE notifications
(
    notification_id UUID PRIMARY KEY,
    recipient_id    UUID         NOT NULL,
    entity_id       UUID,
    message         VARCHAR(255) NOT NULL,
    type            VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    initiator_id    UUID         NOT NULL,
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users (user_id)
);

CREATE INDEX idx_recipient ON notifications (recipient_id);