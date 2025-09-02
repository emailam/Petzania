ALTER TABLE blocks
    ADD CONSTRAINT check_blocks_no_self CHECK ( blocker_id <> blocks.blocked_id );
ALTER TABLE blocks
    ADD CONSTRAINT uc_blocks_blocker_blocked UNIQUE (blocker_id, blocked_id);