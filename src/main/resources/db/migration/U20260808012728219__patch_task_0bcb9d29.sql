UPDATE tasks
SET status = 'done', updated_at = CURRENT_TIMESTAMP
WHERE id = '0bcb9d29-ad04-4c30-8448-e3cbacf70c4f' AND status = 'failed';