CREATE TABLE IF NOT EXISTS user_action_audit (
    id UUID PRIMARY KEY,
    user_identity VARCHAR(255) NOT NULL,
    action_type VARCHAR(255) NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
