package com.eneik.generated;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class Task0bcb9d29PatchTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testPatchProcessRevertsStatus() throws Exception {
        // Given task 0bcb9d29 is marked as 'done'
        String taskId = "0bcb9d29-ad04-4c30-8448-e3cbacf70c4f";

        // Remove it in case it already exists in the test DB context
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", taskId);

        jdbcTemplate.update(
            "INSERT INTO tasks (id, title, status, github_pr_number, github_pr_state, github_pr_merged, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            taskId, "Test Task", "done", 53, "closed", false
        );

        // When the status patch process runs (execute the exact Flyway script)
        ClassPathResource resource = new ClassPathResource("db/migration/V20260808012728219__patch_task_0bcb9d29.sql");
        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        jdbcTemplate.execute(sql);

        // Then the task status is reverted to an open or failed state
        String newStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM tasks WHERE id = ?", String.class, taskId
        );
        assertEquals("failed", newStatus);

        // Cleanup
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", taskId);
    }
}
