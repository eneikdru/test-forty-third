ALTER TABLE documents ADD COLUMN education_level VARCHAR(100);

-- Seed existing documents with relevant education levels (higher or postgraduate_qualification)
UPDATE documents SET education_level = 'higher' WHERE id = 'd0000000-0000-0000-0000-000000000001';
UPDATE documents SET education_level = 'postgraduate_qualification' WHERE id = 'd0000000-0000-0000-0000-000000000002';
UPDATE documents SET education_level = 'higher' WHERE id = 'd0000000-0000-0000-0000-000000000003';
UPDATE documents SET education_level = 'higher' WHERE id = 'd0000000-0000-0000-0000-000000000004';
