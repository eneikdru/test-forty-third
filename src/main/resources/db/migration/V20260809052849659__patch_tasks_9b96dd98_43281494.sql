UPDATE tasks
SET status = 'failed', updated_at = CURRENT_TIMESTAMP
WHERE (CAST(id AS VARCHAR) LIKE '9b96dd98%' OR CAST(id AS VARCHAR) LIKE '43281494%') AND status = 'done';
