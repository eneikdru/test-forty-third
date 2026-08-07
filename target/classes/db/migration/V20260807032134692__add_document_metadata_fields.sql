ALTER TABLE documents ADD COLUMN document_type VARCHAR(50) DEFAULT 'Other' NOT NULL;
ALTER TABLE documents ADD COLUMN academic_year VARCHAR(50) DEFAULT 'infinite' NOT NULL;
ALTER TABLE documents ADD COLUMN status VARCHAR(50) DEFAULT 'PROJECT' NOT NULL;
ALTER TABLE documents ADD COLUMN program VARCHAR(50) DEFAULT 'both' NOT NULL;
ALTER TABLE documents ADD COLUMN process VARCHAR(50) DEFAULT 'other' NOT NULL;
ALTER TABLE documents ADD COLUMN approval_date DATE;
ALTER TABLE documents ADD COLUMN document_number VARCHAR(100);
ALTER TABLE documents ADD COLUMN responsible_name VARCHAR(255);
ALTER TABLE documents ADD COLUMN responsible_title VARCHAR(255);
ALTER TABLE documents ADD COLUMN responsible_unit VARCHAR(255);
ALTER TABLE documents ADD COLUMN decommissioned_at TIMESTAMP;
ALTER TABLE documents ADD COLUMN successor_document_id UUID;

ALTER TABLE documents ADD CONSTRAINT fk_documents_successor FOREIGN KEY (successor_document_id) REFERENCES documents(id) ON DELETE SET NULL;

ALTER TABLE documents ADD CONSTRAINT chk_document_type CHECK (document_type IN ('Position', 'Procedure', 'Project', 'Other'));
ALTER TABLE documents ADD CONSTRAINT chk_academic_year CHECK (academic_year IN ('infinite', 'project', 'бессрочно', 'проект') OR academic_year LIKE '____-____' OR academic_year LIKE '____–____');
ALTER TABLE documents ADD CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'PROJECT', 'ARCHIVED'));
ALTER TABLE documents ADD CONSTRAINT chk_program CHECK (program IN ('postgraduate', 'residency', 'both'));
ALTER TABLE documents ADD CONSTRAINT chk_process CHECK (process IN ('admission', 'certification', 'stipends', 'practice', 'result_tracking', 'other'));
