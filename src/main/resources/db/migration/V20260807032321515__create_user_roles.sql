CREATE TABLE user_roles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    UNIQUE (user_id, role_name)
);
