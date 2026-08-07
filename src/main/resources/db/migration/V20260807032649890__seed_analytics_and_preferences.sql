-- Seed User Notification Preferences for a few demo users
INSERT INTO user_notification_preferences (id, user_id, telegram_chat_id, max_chat_id, notify_on_document_update, created_at, updated_at)
VALUES ('e0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', '123456789', 'max_user_1', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_notification_preferences (id, user_id, telegram_chat_id, max_chat_id, notify_on_document_update, created_at, updated_at)
VALUES ('e0000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', '987654321', 'max_user_2', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
