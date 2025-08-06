-- V4__add_geo_and_relationship_tables.sql

-- 1) Add geo-coordinates to users
ALTER TABLE users
    ADD COLUMN latitude  DOUBLE PRECISION NOT NULL DEFAULT 0.0;
ALTER TABLE users
    ADD COLUMN longitude DOUBLE PRECISION NOT NULL DEFAULT 0.0;

-- 2) Create follows table
CREATE TABLE follows (
                         id          UUID PRIMARY KEY,
                         follower_id UUID NOT NULL,
                         followed_id UUID NOT NULL,
                         created_at  TIMESTAMP    NOT NULL,
                         CONSTRAINT fk_follow_follower
                             FOREIGN KEY (follower_id) REFERENCES users(user_id) ON DELETE CASCADE,
                         CONSTRAINT fk_follow_followed
                             FOREIGN KEY (followed_id)  REFERENCES users(user_id) ON DELETE CASCADE,
                         CONSTRAINT uk_follow_follower_followed
                             UNIQUE (follower_id, followed_id)
);
CREATE INDEX idx_follow_follower ON follows(follower_id);
CREATE INDEX idx_follow_followed ON follows(followed_id);

-- 3) Create friendships table
CREATE TABLE friendships (
                             id         UUID PRIMARY KEY,
                             user1_id   UUID NOT NULL,
                             user2_id   UUID NOT NULL,
                             created_at TIMESTAMP    NOT NULL,
                             CONSTRAINT fk_friendship_user1
                                 FOREIGN KEY (user1_id) REFERENCES users(user_id) ON DELETE CASCADE,
                             CONSTRAINT fk_friendship_user2
                                 FOREIGN KEY (user2_id) REFERENCES users(user_id) ON DELETE CASCADE,
                             CONSTRAINT uk_friendship_user1_user2
                                 UNIQUE (user1_id, user2_id)
);
CREATE INDEX idx_friendship_user1       ON friendships(user1_id);
CREATE INDEX idx_friendship_user2       ON friendships(user2_id);
CREATE INDEX idx_friendship_user1_user2 ON friendships(user1_id, user2_id);

-- 4) Add geo-coordinates to pet_posts
ALTER TABLE pet_posts
    ADD COLUMN latitude  DOUBLE PRECISION NOT NULL DEFAULT 0.0;
ALTER TABLE pet_posts
    ADD COLUMN longitude DOUBLE PRECISION NOT NULL DEFAULT 0.0;

-- 5) Create pet_post_interests join table
CREATE TABLE pet_post_interests (
                                    user_id       UUID    NOT NULL,
                                    post_id       UUID    NOT NULL,
                                    interest_type VARCHAR(32) NOT NULL,
                                    created_at    TIMESTAMP    NOT NULL,
                                    CONSTRAINT pk_pet_post_interest
                                        PRIMARY KEY (user_id, post_id),
                                    CONSTRAINT fk_interest_user
                                        FOREIGN KEY (user_id)   REFERENCES users(user_id)     ON DELETE CASCADE,
                                    CONSTRAINT fk_interest_post
                                        FOREIGN KEY (post_id)   REFERENCES pet_posts(post_id) ON DELETE CASCADE
);
