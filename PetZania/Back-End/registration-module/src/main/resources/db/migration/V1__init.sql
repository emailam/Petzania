-- USERS TABLE
CREATE TABLE users (
                       user_id UUID PRIMARY KEY,
                       username VARCHAR(32) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       login_times INTEGER DEFAULT 0,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       name VARCHAR(255),
                       bio TEXT,
                       profile_picture_url VARCHAR(255),
                       phone_number VARCHAR(20),
                       verification_code VARCHAR(10),
                       reset_code VARCHAR(10),
                       reset_code_expiration_time TIMESTAMP,
                       expiration_time TIMESTAMP,
                       verified BOOLEAN NOT NULL DEFAULT FALSE,
                       online BOOLEAN NOT NULL DEFAULT FALSE,
                       is_blocked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_verified ON users(verified);
CREATE INDEX idx_is_blocked ON users(is_blocked);

-- ADMINS TABLE
CREATE TABLE admins (
                        admin_id UUID PRIMARY KEY,
                        username VARCHAR(32) NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL,
                        role VARCHAR(32) NOT NULL
);

CREATE INDEX idx_admin_username ON admins(username);
CREATE INDEX idx_admin_role ON admins(role);

-- PETS TABLE
CREATE TABLE pets (
                      pet_id UUID PRIMARY KEY,
                      name VARCHAR(255) NOT NULL,
                      description TEXT,
                      gender VARCHAR(16) NOT NULL,
                      date_of_birth DATE NOT NULL,
                      breed VARCHAR(32) NOT NULL,
                      species VARCHAR(32) NOT NULL,
                      user_id UUID NOT NULL,
                      CONSTRAINT fk_pet_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_pet_user ON pets(user_id);
CREATE INDEX idx_pet_species ON pets(species);
CREATE INDEX idx_pet_breed ON pets(breed);
CREATE INDEX idx_pet_gender ON pets(gender);

-- PETS VACCINES URLS
CREATE TABLE pets_vaccines_urls (
                                    pet_id UUID NOT NULL,
                                    vaccine_url VARCHAR(255),
                                    CONSTRAINT fk_vaccine_pet FOREIGN KEY (pet_id) REFERENCES pets(pet_id) ON DELETE CASCADE
);

-- PETS PICTURES URLS
CREATE TABLE pets_pictures_urls (
                                    pet_id UUID NOT NULL,
                                    picture_url VARCHAR(255),
                                    CONSTRAINT fk_picture_pet FOREIGN KEY (pet_id) REFERENCES pets(pet_id) ON DELETE CASCADE
);

-- BLOCKS TABLE
CREATE TABLE blocks (
                        block_id UUID PRIMARY KEY,
                        blocker_id UUID,
                        blocked_id UUID,
                        created_at TIMESTAMP,
                        CONSTRAINT fk_blocker FOREIGN KEY (blocker_id) REFERENCES users(user_id) ON DELETE CASCADE,
                        CONSTRAINT fk_blocked FOREIGN KEY (blocked_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_block_blocker_blocked ON blocks(blocker_id, blocked_id);
CREATE INDEX idx_blocked_users ON blocks(blocker_id);

-- MEDIA TABLE
CREATE TABLE media (
                       media_id UUID PRIMARY KEY,
                       media_key VARCHAR(255),
                       type VARCHAR(64),
                       format VARCHAR(32),
                       uploaded_at TIMESTAMP NOT NULL
);

-- REVOKED REFRESH TOKEN TABLE
CREATE TABLE revoked_refresh_token (
                                       id UUID PRIMARY KEY,
                                       token VARCHAR(255) UNIQUE NOT NULL,
                                       expiration_time TIMESTAMP
);