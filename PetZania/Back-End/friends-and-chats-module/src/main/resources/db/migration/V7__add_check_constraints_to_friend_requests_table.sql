ALTER TABLE friend_requests
    ADD CONSTRAINT check_requests_no_self CHECK ( sender_id <> receiver_id );

ALTER TABLE friend_requests
    ADD CONSTRAINT uc_friend_requests_sender_receiver UNIQUE (sender_id, receiver_id);