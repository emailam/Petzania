-- USERS TABLE
CREATE TABLE users (
                       user_id UUID PRIMARY KEY,
                       username VARCHAR(32) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE
);

CREATE INDEX idx_username ON users(username);

-- ADMINS TABLE
CREATE TABLE admins (
                        admin_id UUID PRIMARY KEY,
                        username VARCHAR(32) NOT NULL UNIQUE,
                        role VARCHAR(20) NOT NULL
);

-- FRIENDSHIPS TABLE
CREATE TABLE friendships (
                             id UUID PRIMARY KEY,
                             user1_id UUID NOT NULL,
                             user2_id UUID NOT NULL,
                             created_at TIMESTAMP,
                             CONSTRAINT fk_friendship_user1 FOREIGN KEY (user1_id) REFERENCES users(user_id) ON DELETE CASCADE,
                             CONSTRAINT fk_friendship_user2 FOREIGN KEY (user2_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_friendship_user1 ON friendships(user1_id);
CREATE INDEX idx_friendship_user2 ON friendships(user2_id);
CREATE INDEX idx_friendship_user1_user2 ON friendships(user1_id, user2_id);

-- FRIEND REQUESTS TABLE
CREATE TABLE friend_requests (
                                 id UUID PRIMARY KEY,
                                 sender_id UUID NOT NULL,
                                 receiver_id UUID NOT NULL,
                                 created_at TIMESTAMP,
                                 CONSTRAINT fk_friend_request_sender FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                 CONSTRAINT fk_friend_request_receiver FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_friend_request_sender_receiver ON friend_requests(sender_id, receiver_id);

-- FOLLOWS TABLE
CREATE TABLE follows (
                         id UUID PRIMARY KEY,
                         follower_id UUID NOT NULL,
                         followed_id UUID NOT NULL,
                         created_at TIMESTAMP,
                         CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES users(user_id) ON DELETE CASCADE,
                         CONSTRAINT fk_follow_followed FOREIGN KEY (followed_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_follow_followed ON follows(followed_id);
CREATE INDEX idx_follow_follower ON follows(follower_id);

-- BLOCKS TABLE
CREATE TABLE blocks (
                        id UUID PRIMARY KEY,
                        blocker_id UUID,
                        blocked_id UUID,
                        created_at TIMESTAMP,
                        CONSTRAINT fk_blocker FOREIGN KEY (blocker_id) REFERENCES users(user_id) ON DELETE CASCADE,
                        CONSTRAINT fk_blocked FOREIGN KEY (blocked_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_block_blocker_blocked ON blocks(blocker_id, blocked_id);
CREATE INDEX idx_blocked_users ON blocks(blocker_id);

-- CHATS TABLE
CREATE TABLE chats (
                       chat_id UUID PRIMARY KEY,
                       user1_id UUID NOT NULL,
                       user2_id UUID NOT NULL,
                       created_at TIMESTAMP NOT NULL,
                       last_message_timestamp TIMESTAMP NOT NULL,
                       CONSTRAINT fk_chat_user1 FOREIGN KEY (user1_id) REFERENCES users(user_id) ON DELETE CASCADE,
                       CONSTRAINT fk_chat_user2 FOREIGN KEY (user2_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_user1 ON chats(user1_id);
CREATE INDEX idx_user2 ON chats(user2_id);

-- USER CHATS TABLE
CREATE TABLE user_chats (
                            user_chat_id UUID PRIMARY KEY,
                            chat_id UUID NOT NULL,
                            user_id UUID NOT NULL,
                            pinned BOOLEAN NOT NULL DEFAULT FALSE,
                            unread INTEGER NOT NULL DEFAULT 0,
                            muted BOOLEAN NOT NULL DEFAULT FALSE,
                            CONSTRAINT fk_user_chat_chat FOREIGN KEY (chat_id) REFERENCES chats(chat_id) ON DELETE CASCADE,
                            CONSTRAINT fk_user_chat_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_user ON user_chats(user_id);
CREATE INDEX idx_chat ON user_chats(chat_id);
CREATE INDEX idx_pinned ON user_chats(pinned);
CREATE INDEX idx_unread ON user_chats(unread);
CREATE INDEX idx_muted ON user_chats(muted);

-- MESSAGES TABLE
CREATE TABLE messages (
                          message_id UUID PRIMARY KEY,
                          chat_id UUID NOT NULL,
                          sender_id UUID NOT NULL,
                          content TEXT,
                          reply_to_id UUID,
                          sent_at TIMESTAMP NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          is_file BOOLEAN NOT NULL,
                          is_edited BOOLEAN NOT NULL DEFAULT FALSE,
                          CONSTRAINT fk_message_chat FOREIGN KEY (chat_id) REFERENCES chats(chat_id) ON DELETE CASCADE,
                          CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
                          CONSTRAINT fk_message_reply_to FOREIGN KEY (reply_to_id) REFERENCES messages(message_id)
);

CREATE INDEX idx_message_chat ON messages(chat_id);
CREATE INDEX idx_message_sender ON messages(sender_id);

-- MESSAGE REACTIONS TABLE
CREATE TABLE message_reactions (
                                   message_reaction_id UUID PRIMARY KEY,
                                   user_id UUID NOT NULL,
                                   message_id UUID NOT NULL,
                                   reaction_type VARCHAR(20) NOT NULL,
                                   CONSTRAINT fk_reaction_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                   CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES messages(message_id) ON DELETE CASCADE,
                                   UNIQUE (user_id, message_id)
);