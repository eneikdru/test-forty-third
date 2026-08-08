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
public class TaskC2afa8f3Ed682fbePatchTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testPatchProcessRevertsStatuses() throws Exception {
        String task1 = "c2afa8f3-ad04-4c30-8448-e3cbacf70c4f";
        String task2 = "ed682fbe-ad04-4c30-8448-e3cbacf70c4f";

        // Remove them if they already exist
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", task1);
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", task2);

        // Insert initial 'done' rows
        jdbcTemplate.update(
            "INSERT INTO tasks (id, title, status, github_pr_number, github_pr_state, github_pr_merged, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            task1, "Task c2afa8f3", "done", 101, "closed", false
        );
        jdbcTemplate.update(
            "INSERT INTO tasks (id, title, status, github_pr_number, github_pr_state, github_pr_merged, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            task2, "Task ed682fbe", "done", 102, "closed", false
        );

        // Execute migration
        ClassPathResource resource = new ClassPathResource("db/migration/V20260808022839735__patch_tasks_c2afa8f3_ed682fbe.sql");
        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        jdbcTemplate.execute(sql);

        // Verify status is changed to 'failed'
        String status1 = jdbcTemplate.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, task1);
        String status2 = jdbcTemplate.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, task2);

        assertEquals("failed", status1);
        assertEquals("failed", status2);

        // Clean up
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", task1);
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", task2);
    }
}
