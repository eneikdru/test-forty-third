CREATE TABLE tags (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE document_tags (
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, tag_id)
);

-- Seed Budget and Load tags
INSERT INTO tags (id, name, created_at) VALUES
('f9cb24e0-798c-4f76-8e50-48e0bfef1a25', 'Budget', CURRENT_TIMESTAMP),
('03a11883-8a39-4cb4-bd9f-2da9e9be41f2', 'Load', CURRENT_TIMESTAMP);

-- Seed Economist and HR roles
INSERT INTO roles (id, name, description, created_at) VALUES
('a7f9a1cb-7080-4cfc-b844-332308736e4b', 'Economist', 'Financial specialist responsible for budgets and planning', CURRENT_TIMESTAMP),
('9579738b-82ef-45d6-8488-8bb82ff63378', 'HR', 'Human resources specialist responsible for roles and staffing', CURRENT_TIMESTAMP);
