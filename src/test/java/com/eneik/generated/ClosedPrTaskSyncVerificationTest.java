package com.eneik.generated;

import com.eneik.generated.model.Task;
import com.eneik.generated.repository.TaskRepository;
import com.eneik.generated.service.GitHubService;
import com.eneik.generated.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class ClosedPrTaskSyncVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        List<Task> existing = taskRepository.findAll();
        System.out.println("DIAGNOSTIC SETUP: Existing tasks count: " + existing.size());
        for (Task t : existing) {
            System.out.println("DIAGNOSTIC SETUP TASK: id=" + t.getId() + " title=" + t.getTitle() + " status=" + t.getStatus() + " pr=" + t.getGithubPrNumber() + " state=" + t.getGithubPrState() + " merged=" + t.getGithubPrMerged());
        }
        taskRepository.deleteAll();
        gitHubService.clearRegistry();
    }

    /**
     * Acceptance Criteria 1:
     * Given the test environment,
     * When a simulated PR closed event (without merge) is received,
     * Then the corresponding task status in the database is verified to not be 'done'.
     */
    @Test
    public void testSimulatedPrClosedWithoutMergeVerifyTaskNotDone() throws Exception {
        // Given an active task associated with GitHub PR 8001
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 8001 - Active", "in_progress", 8001, "open", false);
        taskRepository.saveAndFlush(task);

        // When a simulated PR closed event payload (without merge) is received via Webhook
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "closed");
        payload.put("number", 8001);

        Map<String, Object> prDetails = new HashMap<>();
        prDetails.put("number", 8001);
        prDetails.put("state", "closed");
        prDetails.put("merged", false);
        payload.put("pull_request", prDetails);

        mockMvc.perform(post("/api/v1/integrations/github/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.updatedTasksCount", is(1)));

        // Then the corresponding task status in the database is verified to not be 'done'
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertNotEquals("done", reloaded.getStatus(), "Task status should not be 'done'");
        assertEquals("failed", reloaded.getStatus(), "Task status should be 'failed'");
        assertEquals("closed", reloaded.getGithubPrState());
        assertFalse(reloaded.getGithubPrMerged());
    }

    /**
     * Acceptance Criteria 2:
     * Given the E2E test setup,
     * When a task is processed and its PR is rejected,
     * Then the integration confirms the task flow halts or resets correctly.
     */
    @Test
    public void testTaskProcessedAndPrRejectedVerifyTaskFlowResetsOrHalts() {
        // 1. Reset Flow:
        // When a task is internally marked as "done" but the background/scheduler processes it and finds its PR is rejected (unmerged)
        UUID doneTaskId = UUID.randomUUID();
        Task doneTask = new Task(doneTaskId, "Done Task to Reset", "done", 8002, "open", false);
        taskRepository.saveAndFlush(doneTask);

        // Register GitHub truth: PR 8002 is closed and NOT merged (PR rejected)
        gitHubService.registerPrStatus(8002, "closed", false);

        System.out.println("DIAGNOSTIC: Before sync, task in repo status is: " + taskRepository.findById(doneTaskId).orElseThrow().getStatus());

        // Process synchronization
        int updatedCount = taskService.syncTaskStatusesWithGitHub();

        System.out.println("DIAGNOSTIC: sync returned count: " + updatedCount);

        // Then the task resets correctly (its status reverts from 'done' to 'failed')
        Task reloadedDoneTask = taskRepository.findById(doneTaskId).orElseThrow();
        System.out.println("DIAGNOSTIC: After sync, reloadedDoneTask status is: " + reloadedDoneTask.getStatus());
        System.out.println("DIAGNOSTIC: After sync, reloadedDoneTask githubPrState is: " + reloadedDoneTask.getGithubPrState());
        System.out.println("DIAGNOSTIC: After sync, reloadedDoneTask githubPrMerged is: " + reloadedDoneTask.getGithubPrMerged());

        assertEquals("failed", reloadedDoneTask.getStatus(), "Done task should be reset/reverted to 'failed'");
        assertEquals("closed", reloadedDoneTask.getGithubPrState());
        assertFalse(reloadedDoneTask.getGithubPrMerged());

        // 2. Halt Flow:
        // When an active task is processed and its PR is closed and unmerged (PR rejected)
        UUID activeTaskId = UUID.randomUUID();
        Task activeTask = new Task(activeTaskId, "Active Task to Halt", "in_progress", 8003, "open", false);
        taskRepository.saveAndFlush(activeTask);

        // Register GitHub truth: PR 8003 is closed and NOT merged
        gitHubService.registerPrStatus(8003, "closed", false);

        // Process synchronization
        int updatedActiveCount = taskService.syncTaskStatusesWithGitHub();
        assertEquals(1, updatedActiveCount, "Task's database state should be updated to match GitHub truth");

        // Then the task flow halts correctly (the task remains in its active status, NOT transitioning to 'done')
        Task reloadedActiveTask = taskRepository.findById(activeTaskId).orElseThrow();
        assertNotEquals("done", reloadedActiveTask.getStatus(), "Active task flow should halt (must not be 'done')");
        assertEquals("in_progress", reloadedActiveTask.getStatus(), "Active task should retain its active status");
        assertEquals("closed", reloadedActiveTask.getGithubPrState());
        assertFalse(reloadedActiveTask.getGithubPrMerged());
    }
}
