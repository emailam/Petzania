-- USERS TABLE
CREATE TABLE users (
                       user_id UUID PRIMARY KEY,
                       username VARCHAR(32) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE
);

CREATE INDEX idx_user_id ON users(user_id);

-- ADMINS TABLE
CREATE TABLE admins (
                        admin_id UUID PRIMARY KEY,
                        username VARCHAR(32) NOT NULL UNIQUE,
                        role VARCHAR(32) NOT NULL
);

-- NOTIFICATIONS TABLE
CREATE TABLE notifications (
                               notification_id UUID PRIMARY KEY,
                               recipient_id UUID NOT NULL,
                               message VARCHAR(500) NOT NULL,
                               type VARCHAR(32) NOT NULL,
                               status VARCHAR(32) NOT NULL,
                               created_at TIMESTAMP NOT NULL,
                               initiator_id UUID NOT NULL,
                               CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users(user_id)
);

CREATE INDEX idx_recipient ON notifications(recipient_id);

-- NOTIFICATION ATTRIBUTES TABLE (for Map<String, String> attributes)
CREATE TABLE notification_attributes (
                                         notification_id UUID NOT NULL,
                                         attribute_key VARCHAR(255) NOT NULL,
                                         attribute_value VARCHAR(1000),
                                         CONSTRAINT fk_notification_attr_notification FOREIGN KEY (notification_id) REFERENCES notifications(notification_id) ON DELETE CASCADE
);

-- (Optional) If you want to enforce unique keys per notification:
-- ALTER TABLE notification_attributes ADD CONSTRAINT uq_notification_attr UNIQUE (notification_id, attribute_key);