ALTER TABLE notifications
    ADD CONSTRAINT check_notifications_status_enum CHECK ( status in ('READ', 'UNREAD'));

ALTER TABLE notifications
    ADD CONSTRAINT check_notifications_type_enum CHECK ( type IN (
                                                                  'FRIEND_REQUEST_RECEIVED',
                                                                  'FRIEND_REQUEST_ACCEPTED',
                                                                  'FRIEND_REQUEST_WITHDRAWN',
                                                                  'NEW_FOLLOWER',
                                                                  'PET_POST_LIKED',
                                                                  'PET_POST_DELETED'
        ))