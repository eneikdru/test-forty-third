UPDATE tasks
SET status = 'failed', updated_at = CURRENT_TIMESTAMP
WHERE id = '9df57359-ad04-4c30-8448-e3cbacf70c4f' AND status = 'done';
