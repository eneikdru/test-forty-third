-- Flyway migration V20260807032311241 reserved by orchestrator
-- Ensures schema supports external resource metadata and notification subscriptions for Telegram and Max

CREATE TABLE IF NOT EXISTS document_lms_metadata (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    lms_provider VARCHAR(100) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    external_url VARCHAR(1024),
    metadata_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (document_id, lms_provider)
);

CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    telegram_chat_id VARCHAR(255),
    max_chat_id VARCHAR(255),
    notify_on_document_update BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS analytics_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id UUID,
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    search_query VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
