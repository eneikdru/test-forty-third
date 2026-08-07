-- Add strictly typed columns to documents table
ALTER TABLE documents ADD COLUMN document_type VARCHAR(50) DEFAULT 'Other' NOT NULL;
ALTER TABLE documents ADD COLUMN academic_year VARCHAR(100) DEFAULT '2026–2027' NOT NULL;
ALTER TABLE documents ADD COLUMN program VARCHAR(50) DEFAULT 'both' NOT NULL;
ALTER TABLE documents ADD COLUMN process VARCHAR(50) DEFAULT 'other' NOT NULL;
ALTER TABLE documents ADD COLUMN approval_date DATE;
ALTER TABLE documents ADD COLUMN document_number VARCHAR(100);

-- Add strict check constraints to documents table
ALTER TABLE documents ADD CONSTRAINT chk_documents_type CHECK (document_type IN ('Position', 'Procedure', 'Project', 'Other'));
ALTER TABLE documents ADD CONSTRAINT chk_documents_academic_year CHECK (academic_year IN ('2024-2025', '2025-2026', '2026-2027', '2024–2025', '2025–2026', '2026–2027', 'бессрочно', 'проект', 'infinite', 'project'));
ALTER TABLE documents ADD CONSTRAINT chk_documents_program CHECK (program IN ('postgraduate', 'residency', 'both'));
ALTER TABLE documents ADD CONSTRAINT chk_documents_process CHECK (process IN ('admission', 'certification', 'stipends', 'practice', 'result_tracking', 'other'));

-- Add check constraint for status in document_versions table
ALTER TABLE document_versions ADD CONSTRAINT chk_document_versions_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DRAFT', 'PROJECT', 'действующий', 'проект', 'архив'));
