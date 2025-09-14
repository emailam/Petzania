ALTER TABLE admins
    ADD CONSTRAINT check_admins_username_length CHECK (char_length(username) >= 5 AND char_length(username) <= 32);

ALTER TABLE admins
    ADD CONSTRAINT check_admins_role_enum CHECK (role IN ('ADMIN', 'SUPER_ADMIN'));