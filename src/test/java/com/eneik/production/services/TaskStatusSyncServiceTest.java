package com.eneik.production.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(classes = {com.eneik.generated.Application.class, com.eneik.production.config.ProductionConfig.class})
@Transactional
public class TaskStatusSyncServiceTest {

    @Autowired
    private TaskStatusSyncService taskStatusSyncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID taskId;

    @BeforeEach
    public void setUp() {
        taskId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String sql = "INSERT INTO sync_tasks (id, github_pr_number, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, taskId, "123", "done");
    }

    @Test
    public void testSyncTaskStatus_ClosedAndUnmerged_RevertsToIncomplete() {
        boolean updated = taskStatusSyncService.syncTaskStatusWithGitHub(taskId, true, false);

        assertTrue(updated, "The task status should have been updated.");

        String status = jdbcTemplate.queryForObject("SELECT status FROM sync_tasks WHERE id = ?", String.class, taskId);
        assertEquals("incomplete", status, "The status should be reverted to incomplete.");
    }

    @Test
    public void testSyncTaskStatus_NotDone_DoesNotUpdate() {
        UUID newTaskId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        jdbcTemplate.update("INSERT INTO sync_tasks (id, github_pr_number, status) VALUES (?, ?, ?)", newTaskId, "456", "in_progress");

        boolean updated = taskStatusSyncService.syncTaskStatusWithGitHub(newTaskId, true, false);

        assertFalse(updated, "The task status should not be updated since it wasn't done.");

        String status = jdbcTemplate.queryForObject("SELECT status FROM sync_tasks WHERE id = ?", String.class, newTaskId);
        assertEquals("in_progress", status, "The status should remain in_progress.");
    }

    @Test
    public void testSyncTaskStatus_ClosedAndMerged_DoesNotUpdate() {
        boolean updated = taskStatusSyncService.syncTaskStatusWithGitHub(taskId, true, true);

        assertFalse(updated, "The task status should not be updated if the PR was merged.");

        String status = jdbcTemplate.queryForObject("SELECT status FROM sync_tasks WHERE id = ?", String.class, taskId);
        assertEquals("done", status, "The status should remain done.");
    }

    @Test
    public void testSyncTaskStatus_NotClosed_DoesNotUpdate() {
        boolean updated = taskStatusSyncService.syncTaskStatusWithGitHub(taskId, false, false);

        assertFalse(updated, "The task status should not be updated if the PR is not closed.");

        String status = jdbcTemplate.queryForObject("SELECT status FROM sync_tasks WHERE id = ?", String.class, taskId);
        assertEquals("done", status, "The status should remain done.");
    }
}
