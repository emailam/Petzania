ALTER TABLE follows
    ADD CONSTRAINT check_follows_no_self CHECK ( follower_id <> followed_id );

ALTER TABLE follows
    ADD CONSTRAINT uc_follows_follower_followed UNIQUE (follower_id, followed_id)