package com.eneik.generated;

import com.eneik.generated.model.Task;
import com.eneik.generated.repository.TaskRepository;
import com.eneik.generated.service.GitHubService;
import com.eneik.generated.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
public class Task9df57359PatchTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private GitHubService gitHubService;

    @Test
    public void testPatchProcessRevertsStatus() throws Exception {
        // Given task 9df57359 is marked as 'done'
        String taskIdStr = "9df57359-ad04-4c30-8448-e3cbacf70c4f";

        // Remove it in case it already exists in the test DB context
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", taskIdStr);

        jdbcTemplate.update(
            "INSERT INTO tasks (id, title, status, github_pr_number, github_pr_state, github_pr_merged, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            taskIdStr, "Task 9df57359", "done", 9001, "closed", false
        );

        // When the status patch process runs (execute the exact Flyway script)
        ClassPathResource resource = new ClassPathResource("db/migration/V20260809101929041__patch_task_9df57359.sql");
        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        jdbcTemplate.execute(sql);

        // Then the task status is reverted to a failed/incomplete state
        String newStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM tasks WHERE id = ?", String.class, taskIdStr
        );
        assertEquals("failed", newStatus);

        // Cleanup
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", taskIdStr);
    }

    @Test
    public void testTaskServiceSyncRevertsDoneTaskWithUnmergedPr() {
        UUID taskId = UUID.fromString("9df57359-ad04-4c30-8448-e3cbacf70c4f");

        // Clean any potential leftover from other tests
        taskRepository.deleteById(taskId);
        taskRepository.flush();

        // Given task starts as 'done' internally with PR #9001
        Task task = new Task(taskId, "Task 9df57359 Sync Test", "done", 9001, "open", false);
        taskRepository.saveAndFlush(task);

        // When the GitHub PR is registered as closed and unmerged
        gitHubService.registerPrStatus(9001, "closed", false);

        // Run synchronization
        int updated = taskService.syncTaskStatusesWithGitHub();
        assertEquals(1, updated, "One task should be synchronized");

        // Then the task is reverted to 'failed' (incomplete) state
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus(), "Task status should be reverted to failed");
        assertEquals("closed", reloaded.getGithubPrState());
        assertFalse(reloaded.getGithubPrMerged());

        // Cleanup
        taskRepository.deleteById(taskId);
        taskRepository.flush();
    }
}
