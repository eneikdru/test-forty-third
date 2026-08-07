-- Flyway migration V20260807010025515__create_financial_and_workload_schema.sql
-- Define schema for budgets, workloads, scholarships, and visibility rules

CREATE TABLE visibility_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(255) NOT NULL, -- e.g., 'ADMIN', 'CONTENT_MANAGER', 'TEACHER', 'STUDENT'
    allowed_actions VARCHAR(255) NOT NULL, -- e.g., 'READ', 'WRITE', 'ALL'
    description VARCHAR(255)
);

CREATE TABLE budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(255) NOT NULL,
    allocated_amount DECIMAL(19, 2) NOT NULL,
    spent_amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL, -- e.g., 'NORMAL', 'EXCEEDED'
    visibility_rule_id BIGINT,
    CONSTRAINT fk_budgets_visibility FOREIGN KEY (visibility_rule_id) REFERENCES visibility_rules(id) ON DELETE SET NULL
);

CREATE TABLE workloads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instructor_name VARCHAR(255) NOT NULL,
    hours_allocated INT NOT NULL,
    hours_completed INT NOT NULL,
    semester VARCHAR(50) NOT NULL,
    visibility_rule_id BIGINT,
    CONSTRAINT fk_workloads_visibility FOREIGN KEY (visibility_rule_id) REFERENCES visibility_rules(id) ON DELETE SET NULL
);

CREATE TABLE scholarships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_name VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    type VARCHAR(100) NOT NULL, -- e.g., 'ACADEMIC', 'SOCIAL'
    status VARCHAR(50) NOT NULL, -- e.g., 'APPROVED', 'PENDING'
    visibility_rule_id BIGINT,
    CONSTRAINT fk_scholarships_visibility FOREIGN KEY (visibility_rule_id) REFERENCES visibility_rules(id) ON DELETE SET NULL
);
