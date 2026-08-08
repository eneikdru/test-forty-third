UPDATE tasks
SET status = 'done', updated_at = CURRENT_TIMESTAMP
WHERE id IN ('c2afa8f3-ad04-4c30-8448-e3cbacf70c4f', 'ed682fbe-ad04-4c30-8448-e3cbacf70c4f') AND status = 'failed';
