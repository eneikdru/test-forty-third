package com.eneik.production.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TaskStatusSyncService {

    private final JdbcTemplate jdbcTemplate;

    public TaskStatusSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public boolean syncTaskStatusWithGitHub(UUID taskId, boolean isPrClosed, boolean isPrMerged) {
        if (taskId == null) {
            return false;
        }
        if (isPrClosed && !isPrMerged) {
            String sql = "UPDATE sync_tasks SET status = 'failed' WHERE id = ? AND status != 'failed'";
            int updatedRows = jdbcTemplate.update(sql, taskId);
            return updatedRows > 0;
        }
        return false;
    }
}
