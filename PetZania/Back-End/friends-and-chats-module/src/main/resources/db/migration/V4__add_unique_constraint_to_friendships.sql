ALTER TABLE friendships
    ADD CONSTRAINT uc_friendships_user1_user2 UNIQUE (user1_id, user2_id);

ALTER TABLE friendships
    ADD CONSTRAINT check_friends_no_self CHECK (user1_id <> user2_id)