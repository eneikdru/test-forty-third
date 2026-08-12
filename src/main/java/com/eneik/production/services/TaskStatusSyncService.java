package com.eneik.production.services;

import com.eneik.generated.util.TimeProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.UUID;

@Service
public class TaskStatusSyncService {

    private final JdbcTemplate jdbcTemplate;
    private final TimeProvider timeProvider;

    public TaskStatusSyncService(JdbcTemplate jdbcTemplate, TimeProvider timeProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public boolean syncTaskStatusWithGitHub(UUID taskId, boolean isPrClosed, boolean isPrMerged) {
        if (taskId == null) {
            return false;
        }
        if (isPrClosed && !isPrMerged) {
            // First, evaluate the review concerns and assign the rootCausePatternId to the event
            evaluateReviewConcerns(taskId, isPrClosed, isPrMerged);

            // Then, perform the task status transition to 'failed'
            String sql = "UPDATE sync_tasks SET status = 'failed', updated_at = ? WHERE id = ? AND status != 'failed'";
            int updatedRows = jdbcTemplate.update(sql, Timestamp.valueOf(timeProvider.now()), taskId);
            return updatedRows > 0;
        }
        return false;
    }

    /**
     * Evaluates the review concerns for a task status mismatch defect event
     * and assigns the 'reviewConcerns' rootCausePatternId to it.
     */
    @Transactional
    public boolean evaluateReviewConcerns(UUID taskId, boolean isPrClosed, boolean isPrMerged) {
        if (taskId == null) {
            return false;
        }
        if (isPrClosed && !isPrMerged) {
            String sql = "UPDATE sync_tasks SET root_cause_pattern_id = 'reviewConcerns', updated_at = ? WHERE id = ?";
            int updatedRows = jdbcTemplate.update(sql, Timestamp.valueOf(timeProvider.now()), taskId);
            return updatedRows > 0;
        }
        return false;
    }

    /**
     * Patches any existing uncategorized mismatch events (failed tasks with null rootCausePatternId)
     * by assigning them the 'reviewConcerns' rootCausePatternId.
     */
    @Transactional
    public int categorizeUncategorizedMismatchEvents() {
        String sql = "UPDATE sync_tasks SET root_cause_pattern_id = 'reviewConcerns', updated_at = ? WHERE status = 'failed' AND root_cause_pattern_id IS NULL";
        return jdbcTemplate.update(sql, Timestamp.valueOf(timeProvider.now()));
    }
}
