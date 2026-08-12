package com.eneik.production.services;

import com.eneik.generated.util.TimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {com.eneik.generated.Application.class, com.eneik.production.config.ProductionConfig.class})
@Transactional
public class TaskStatusSyncServiceTest {

    @Autowired
    private TaskStatusSyncService taskStatusSyncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TimeProvider timeProvider;

    private UUID taskId;
    private LocalDateTime fixedTime;

    @BeforeEach
    public void setUp() {
        taskId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String sql = "INSERT INTO sync_tasks (id, github_pr_number, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, taskId, "123", "done");

        fixedTime = LocalDateTime.of(2026, 8, 11, 12, 0, 0);
        timeProvider.setFixedDateTime(fixedTime);
    }

    @AfterEach
    public void tearDown() {
        timeProvider.reset();
    }

    @Test
    public void testSyncTaskStatus_ClosedAndUnmerged_TransitionsToFailed() {
        boolean updated = taskStatusSyncService.syncTaskStatusWithGitHub(taskId, true, false);

        assertTrue(updated, "The task status should have been updated.");

        String status = jdbcTemplate.queryForObject("SELECT status FROM sync_tasks WHERE id = ?", String.class, taskId);
        assertEquals("failed", status, "The status should be transitioned to failed.");

        Timestamp updatedAt = jdbcTemplate.queryForObject("SELECT updated_at FROM sync_tasks WHERE id = ?", Timestamp.class, taskId);
        assertNotNull(updatedAt, "The updated_at timestamp should not be null.");
        assertEquals(Timestamp.valueOf(fixedTime), updatedAt, "The updated_at timestamp should match the fixed time from TimeProvider.");

        String rootCausePatternId = jdbcTemplate.queryForObject("SELECT root_cause_pattern_id FROM sync_tasks WHERE id = ?", String.class, taskId);
        assertEquals("reviewConcerns", rootCausePatternId, "The root_cause_pattern_id should be assigned as reviewConcerns.");
    }

    @Test
    public void testSyncTaskStatus_InProgress_RetainsActiveStatus() {
        UUID newTaskId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        jdbcTemplate.update("INSERT INTO sync_tasks (id, github_pr_number, status) VALUES (?, ?, ?)", newTaskId, "456", "in_progress");

        boolean updated = taskStatusSyncService.syncTaskStatusWithGitHub(newTaskId, true, false);

        assertFalse(updated, "The task status should not be updated since it is in an active (non-done) status.");

        String status = jdbcTemplate.queryForObject("SELECT status FROM sync_tasks WHERE id = ?", String.class, newTaskId);
        assertEquals("in_progress", status, "The status should remain in_progress.");

        String rootCausePatternId = jdbcTemplate.queryForObject("SELECT root_cause_pattern_id FROM sync_tasks WHERE id = ?", String.class, newTaskId);
        assertEquals(null, rootCausePatternId, "The root_cause_pattern_id should remain null.");
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

    @Test
    public void testSyncTaskStatus_NullTaskId_ReturnsFalse() {
        boolean updated = taskStatusSyncService.syncTaskStatusWithGitHub(null, true, false);
        assertFalse(updated, "The task status sync should return false when taskId is null.");
    }

    @Test
    public void testSyncTaskStatus_AlreadyFailed_ReturnsFalseAndNoRedundantUpdate() {
        UUID failedTaskId = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");
        jdbcTemplate.update("INSERT INTO sync_tasks (id, github_pr_number, status) VALUES (?, ?, ?)", failedTaskId, "999", "failed");

        boolean updated = taskStatusSyncService.syncTaskStatusWithGitHub(failedTaskId, true, false);
        assertFalse(updated, "Should return false since the task is already failed.");

        String status = jdbcTemplate.queryForObject("SELECT status FROM sync_tasks WHERE id = ?", String.class, failedTaskId);
        assertEquals("failed", status, "The status should remain failed.");
    }

    @Test
    public void testSyncTaskStatus_TaskIdNotFound_ReturnsFalse() {
        UUID nonExistentTaskId = UUID.fromString("123e4567-e89b-12d3-a456-426614174003");
        boolean updated = taskStatusSyncService.syncTaskStatusWithGitHub(nonExistentTaskId, true, false);
        assertFalse(updated, "Should return false since the task ID does not exist in the database.");
    }
}
