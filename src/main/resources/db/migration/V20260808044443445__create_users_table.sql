CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

-- Seed default corporate users
INSERT INTO users (id, username, password_hash) VALUES ('11111111-1111-1111-1111-111111111111', 'economist', '$2a$10$6iBtSQaIIfnBMa4/Qn.X5uPQuylTlEBBmaI0TA1MHt79AFwtluD1S');
INSERT INTO users (id, username, password_hash) VALUES ('22222222-2222-2222-2222-222222222222', 'teacher', '$2a$10$6iBtSQaIIfnBMa4/Qn.X5uPQuylTlEBBmaI0TA1MHt79AFwtluD1S');
INSERT INTO users (id, username, password_hash) VALUES ('33333333-3333-3333-3333-333333333333', 'postgraduate', '$2a$10$6iBtSQaIIfnBMa4/Qn.X5uPQuylTlEBBmaI0TA1MHt79AFwtluD1S');
INSERT INTO users (id, username, password_hash) VALUES ('44444444-4444-4444-4444-444444444444', 'admin', '$2a$10$6iBtSQaIIfnBMa4/Qn.X5uPQuylTlEBBmaI0TA1MHt79AFwtluD1S');

-- Seed user role mappings in existing user_roles table
INSERT INTO user_roles (id, user_id, role_name) VALUES (RANDOM_UUID(), '11111111-1111-1111-1111-111111111111', 'Economist');
INSERT INTO user_roles (id, user_id, role_name) VALUES (RANDOM_UUID(), '22222222-2222-2222-2222-222222222222', 'Teacher');
INSERT INTO user_roles (id, user_id, role_name) VALUES (RANDOM_UUID(), '33333333-3333-3333-3333-333333333333', 'Postgraduate');
INSERT INTO user_roles (id, user_id, role_name) VALUES (RANDOM_UUID(), '44444444-4444-4444-4444-444444444444', 'Administrator');
