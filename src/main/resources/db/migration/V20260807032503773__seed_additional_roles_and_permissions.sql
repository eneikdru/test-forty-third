-- Seed additional roles: Teacher, Postgraduate, Resident
INSERT INTO roles (id, name, description) VALUES ('56782341-a1b2-c3d4-e5f6-7890abcdef12', 'Teacher', 'Seeded Teacher role');
INSERT INTO roles (id, name, description) VALUES ('90123456-a1b2-c3d4-e5f6-7890abcdef34', 'Postgraduate', 'Seeded Postgraduate role');
INSERT INTO roles (id, name, description) VALUES ('34567890-a1b2-c3d4-e5f6-7890abcdef56', 'Resident', 'Seeded Resident role');

-- Seed Stipends schema tag
INSERT INTO schema_tags (id, name, description) VALUES ('c1234567-d89a-4b28-bb71-88fc40da9933', 'Stipends', 'Strictly isolated stipends documents');

-- Assign schema tag permissions to roles:
-- Economist (id: df9d1cf3-2287-43cf-be61-b58e658fe7d4) should access Budget, Load, and Stipends
INSERT INTO role_schema_tags (role_id, schema_tag_id) VALUES ('df9d1cf3-2287-43cf-be61-b58e658fe7d4', '2a0e2e5c-7d9a-4c28-bb71-88fc40da9921'); -- Budget
INSERT INTO role_schema_tags (role_id, schema_tag_id) VALUES ('df9d1cf3-2287-43cf-be61-b58e658fe7d4', 'f8a5c376-74b8-4c31-9f9b-6dbb7ee5eb2c'); -- Load
INSERT INTO role_schema_tags (role_id, schema_tag_id) VALUES ('df9d1cf3-2287-43cf-be61-b58e658fe7d4', 'c1234567-d89a-4b28-bb71-88fc40da9933'); -- Stipends

-- Teacher (id: 56782341-a1b2-c3d4-e5f6-7890abcdef12) should access Load and Stipends
INSERT INTO role_schema_tags (role_id, schema_tag_id) VALUES ('56782341-a1b2-c3d4-e5f6-7890abcdef12', 'f8a5c376-74b8-4c31-9f9b-6dbb7ee5eb2c'); -- Load
INSERT INTO role_schema_tags (role_id, schema_tag_id) VALUES ('56782341-a1b2-c3d4-e5f6-7890abcdef12', 'c1234567-d89a-4b28-bb71-88fc40da9933'); -- Stipends

-- Postgraduate (id: 90123456-a1b2-c3d4-e5f6-7890abcdef34) should access Stipends
INSERT INTO role_schema_tags (role_id, schema_tag_id) VALUES ('90123456-a1b2-c3d4-e5f6-7890abcdef34', 'c1234567-d89a-4b28-bb71-88fc40da9933'); -- Stipends

-- Resident (id: 34567890-a1b2-c3d4-e5f6-7890abcdef56) should access Stipends
INSERT INTO role_schema_tags (role_id, schema_tag_id) VALUES ('34567890-a1b2-c3d4-e5f6-7890abcdef56', 'c1234567-d89a-4b28-bb71-88fc40da9933'); -- Stipends
