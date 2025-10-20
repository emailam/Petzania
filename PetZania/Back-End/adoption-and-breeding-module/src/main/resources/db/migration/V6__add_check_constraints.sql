ALTER TABLE users
    ADD CONSTRAINT check_users_username_length CHECK (char_length(username) >= 5 AND char_length(username) <= 32);

ALTER TABLE users
    ADD CONSTRAINT check_users_email_length CHECK (char_length(email) > 0 AND char_length(email) <= 100);

ALTER TABLE users
    ADD CONSTRAINT check_users_email_correctness CHECK (position('@' in email) > 1);

ALTER TABLE admins
    ADD CONSTRAINT check_admins_username_length CHECK (char_length(username) >= 5 AND char_length(username) <= 32);

ALTER TABLE admins
    ADD CONSTRAINT check_admins_role_enum CHECK (role IN ('ADMIN', 'SUPER_ADMIN'));

ALTER TABLE blocks
    ADD CONSTRAINT check_blocks_no_self CHECK ( blocker_id <> blocks.blocked_id );

ALTER TABLE blocks
    ADD CONSTRAINT uc_blocks_blocker_blocked UNIQUE (blocker_id, blocked_id);