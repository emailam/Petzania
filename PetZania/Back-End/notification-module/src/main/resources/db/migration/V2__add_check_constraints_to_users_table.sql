ALTER TABLE users
    ADD CONSTRAINT check_users_username_length CHECK (char_length(username) >= 5 AND char_length(username) <= 32);

ALTER TABLE users
    ADD CONSTRAINT check_users_email_length CHECK (char_length(email) > 0 AND char_length(email) <= 100);

ALTER TABLE users
    ADD CONSTRAINT check_users_email_correctness CHECK (position('@' in email) > 1);
