-- Seed Categories
INSERT INTO categories (id, name, parent_id) VALUES ('f0000000-0000-0000-0000-000000000001', 'Финансы', NULL);
INSERT INTO categories (id, name, parent_id) VALUES ('f0000000-0000-0000-0000-000000000002', 'Кадры', NULL);
INSERT INTO categories (id, name, parent_id) VALUES ('f0000000-0000-0000-0000-000000000003', 'Стипендии', NULL);
INSERT INTO categories (id, name, parent_id) VALUES ('f0000000-0000-0000-0000-000000000004', 'Нагрузка', NULL);
INSERT INTO categories (id, name, parent_id) VALUES ('f0000000-0000-0000-0000-000000000005', 'Бюджет', NULL);

-- Seed Documents
INSERT INTO documents (id, category_id, title, description)
VALUES ('d0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000005', 'Положение о бюджете на 2026-2027 годы', 'Определяет бюджетный цикл на текущий год.');

INSERT INTO documents (id, category_id, title, description)
VALUES ('d0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000004', 'Порядок расчета учебной нагрузки преподавателей', 'Формулы расчета и распределения нагрузки.');

INSERT INTO documents (id, category_id, title, description)
VALUES ('d0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000003', 'Положение о стипендиальном обеспечении', 'Регламентирует выплаты стипендий.');

INSERT INTO documents (id, category_id, title, description)
VALUES ('d0000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000002', 'Положение об оплате труда и штате', 'Штатное расписание и должностные оклады.');

-- Link Documents to Schema Tags
-- '2a0e2e5c-7d9a-4c28-bb71-88fc40da9921' is 'Budget'
-- 'f8a5c376-74b8-4c31-9f9b-6dbb7ee5eb2c' is 'Load'
-- 'c1234567-d89a-4b28-bb71-88fc40da9933' is 'Stipends'
INSERT INTO document_schema_tags (document_id, schema_tag_id) VALUES ('d0000000-0000-0000-0000-000000000001', '2a0e2e5c-7d9a-4c28-bb71-88fc40da9921');
INSERT INTO document_schema_tags (document_id, schema_tag_id) VALUES ('d0000000-0000-0000-0000-000000000002', 'f8a5c376-74b8-4c31-9f9b-6dbb7ee5eb2c');
INSERT INTO document_schema_tags (document_id, schema_tag_id) VALUES ('d0000000-0000-0000-0000-000000000003', 'c1234567-d89a-4b28-bb71-88fc40da9933');
INSERT INTO document_schema_tags (document_id, schema_tag_id) VALUES ('d0000000-0000-0000-0000-000000000004', '2a0e2e5c-7d9a-4c28-bb71-88fc40da9921');

-- Seed Document Versions
INSERT INTO document_versions (id, document_id, version_number, file_url, file_type, status, author_name, changes_summary)
VALUES (RANDOM_UUID(), 'd0000000-0000-0000-0000-000000000001', 1, '/docs/budget-v1.pdf', 'pdf', 'ACTIVE', 'Светлана Романова', 'Начальная версия');

INSERT INTO document_versions (id, document_id, version_number, file_url, file_type, status, author_name, changes_summary)
VALUES (RANDOM_UUID(), 'd0000000-0000-0000-0000-000000000002', 1, '/docs/load-v1.pdf', 'pdf', 'ACTIVE', 'Светлана Романова', 'Начальная версия');

INSERT INTO document_versions (id, document_id, version_number, file_url, file_type, status, author_name, changes_summary)
VALUES (RANDOM_UUID(), 'd0000000-0000-0000-0000-000000000003', 1, '/docs/stipends-v1.pdf', 'pdf', 'ACTIVE', 'Светлана Романова', 'Начальная версия');

INSERT INTO document_versions (id, document_id, version_number, file_url, file_type, status, author_name, changes_summary)
VALUES (RANDOM_UUID(), 'd0000000-0000-0000-0000-000000000004', 1, '/docs/hr-v1.pdf', 'pdf', 'ACTIVE', 'Светлана Романова', 'Начальная версия');
