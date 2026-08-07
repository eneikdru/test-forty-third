-- Create schema_tags table
CREATE TABLE schema_tags (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create document_schema_tags join table
CREATE TABLE document_schema_tags (
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    schema_tag_id UUID NOT NULL REFERENCES schema_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, schema_tag_id)
);

-- Create role_schema_tags join table
CREATE TABLE role_schema_tags (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    schema_tag_id UUID NOT NULL REFERENCES schema_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, schema_tag_id)
);

-- Seed Economist and HR roles
INSERT INTO roles (id, name, description) VALUES ('df9d1cf3-2287-43cf-be61-b58e658fe7d4', 'Economist', 'Seeded Economist role for HR and financial isolation');
INSERT INTO roles (id, name, description) VALUES ('9fbeed1d-fbde-4148-8df0-7a0e70a049e2', 'HR', 'Seeded HR role for HR and financial isolation');

-- Seed Budget and Load schema tags
INSERT INTO schema_tags (id, name, description) VALUES ('2a0e2e5c-7d9a-4c28-bb71-88fc40da9921', 'Budget', 'Strictly isolated budget documents');
INSERT INTO schema_tags (id, name, description) VALUES ('f8a5c376-74b8-4c31-9f9b-6dbb7ee5eb2c', 'Load', 'Strictly isolated load documents');
