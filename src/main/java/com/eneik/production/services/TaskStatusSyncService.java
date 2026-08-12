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
            String sql = "UPDATE sync_tasks SET status = 'failed', updated_at = ?, root_cause_pattern_id = 'reviewConcerns' WHERE id = ? AND status = 'done'";
            int updatedRows = jdbcTemplate.update(sql, Timestamp.valueOf(timeProvider.now()), taskId);
            return updatedRows > 0;
        }
        return false;
    }
}
