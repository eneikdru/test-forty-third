-- Migration: Extend schema for external LMS metadata, notification subscriptions (Telegram and Max user preferences), and analytics events.
-- Flyway version: V20260807032311241

CREATE TABLE external_lms_metadata (
    id UUID PRIMARY KEY,
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
    lms_name VARCHAR(100) NOT NULL, -- e.g., 'Moodle', 'Teachbase'
    external_id VARCHAR(255), -- ID/identifier in the external LMS system
    resource_url VARCHAR(1024) NOT NULL, -- URL to the resource or external link
    sync_status VARCHAR(50) NOT NULL, -- e.g., 'SYNCED', 'PENDING_UPDATE', 'FAILED'
    last_synced_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE telegram_user_preferences (
    user_id UUID PRIMARY KEY,
    telegram_chat_id VARCHAR(255) NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE max_user_preferences (
    user_id UUID PRIMARY KEY,
    max_user_id VARCHAR(255) NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE notification_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    channel VARCHAR(50) NOT NULL, -- e.g., 'TELEGRAM', 'MAX'
    category_id UUID REFERENCES categories(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE analytics_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL, -- e.g., 'VIEW', 'DOWNLOAD', 'SEARCH'
    user_id UUID,
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    search_query TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
