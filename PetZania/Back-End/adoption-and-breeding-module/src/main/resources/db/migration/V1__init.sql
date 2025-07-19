-- USERS TABLE
CREATE TABLE users (
                       user_id UUID PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE
);

CREATE INDEX idx_username ON users(username);

-- ADMINS TABLE
CREATE TABLE admins (
                        admin_id UUID PRIMARY KEY,
                        username VARCHAR(255) NOT NULL UNIQUE,
                        role VARCHAR(32) NOT NULL
);

-- PETS TABLE
CREATE TABLE pets (
                      pet_id UUID PRIMARY KEY,
                      name VARCHAR(255) NOT NULL,
                      description TEXT,
                      gender VARCHAR(16) NOT NULL,
                      date_of_birth DATE NOT NULL,
                      breed VARCHAR(255) NOT NULL,
                      species VARCHAR(32) NOT NULL
);

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

-- PET POSTS TABLE
CREATE TABLE pet_posts (
                           post_id UUID PRIMARY KEY,
                           owner_id UUID NOT NULL,
                           pet_id UUID NOT NULL,
                           post_status VARCHAR(32) NOT NULL,
                           reacts INTEGER NOT NULL DEFAULT 0,
                           description VARCHAR(2000),
                           location VARCHAR(255) NOT NULL,
                           post_type VARCHAR(32) NOT NULL,
                           created_at TIMESTAMP NOT NULL,
                           updated_at TIMESTAMP,
                           CONSTRAINT fk_post_owner FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE,
                           CONSTRAINT fk_post_pet FOREIGN KEY (pet_id) REFERENCES pets(pet_id) ON DELETE CASCADE
);

CREATE INDEX idx_post_owner ON pet_posts(owner_id);
CREATE INDEX idx_post_pet ON pet_posts(pet_id);
CREATE INDEX idx_post_status ON pet_posts(post_status);
CREATE INDEX idx_post_type ON pet_posts(post_type);
CREATE INDEX idx_post_created ON pet_posts(created_at);

-- PET POST REACTIONS (Many-to-Many)
CREATE TABLE pet_post_reactions (
                                    post_id UUID NOT NULL,
                                    user_id UUID NOT NULL,
                                    CONSTRAINT fk_reaction_post FOREIGN KEY (post_id) REFERENCES pet_posts(post_id) ON DELETE CASCADE,
                                    CONSTRAINT fk_reaction_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                    UNIQUE (post_id, user_id)
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