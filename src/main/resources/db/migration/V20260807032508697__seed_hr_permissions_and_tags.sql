-- Seed permissions and schema tags for the HR role (id: '9fbeed1d-fbde-4148-8df0-7a0e70a049e2')
-- Assign 'Budget', 'Load', and 'Stipends' schema tags to HR role.
INSERT INTO role_schema_tags (role_id, schema_tag_id) VALUES ('9fbeed1d-fbde-4148-8df0-7a0e70a049e2', '2a0e2e5c-7d9a-4c28-bb71-88fc40da9921'); -- Budget
INSERT INTO role_schema_tags (role_id, schema_tag_id) VALUES ('9fbeed1d-fbde-4148-8df0-7a0e70a049e2', 'f8a5c376-74b8-4c31-9f9b-6dbb7ee5eb2c'); -- Load
INSERT INTO role_schema_tags (role_id, schema_tag_id) VALUES ('9fbeed1d-fbde-4148-8df0-7a0e70a049e2', 'c1234567-d89a-4b28-bb71-88fc40da9933'); -- Stipends
