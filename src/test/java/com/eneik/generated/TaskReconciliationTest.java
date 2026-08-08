package com.eneik.generated;

import com.eneik.generated.model.Task;
import com.eneik.generated.repository.TaskRepository;
import com.eneik.generated.service.GitHubService;
import com.eneik.generated.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eneik.generated.service.TaskSyncScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TaskReconciliationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private TaskSyncScheduler taskSyncScheduler;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        // Clean up tasks table
        jdbcTemplate.update("DELETE FROM tasks");
        // Clear stub/seed registry in GitHubService
        gitHubService.clearRegistry();
    }

    @Test
    public void testCreateAndGetTaskSuccess() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Test Task Name", "in_progress", 42, "open", false);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(taskId.toString())))
                .andExpect(jsonPath("$.title", is("Test Task Name")))
                .andExpect(jsonPath("$.status", is("in_progress")))
                .andExpect(jsonPath("$.githubPrNumber", is(42)));

        mockMvc.perform(get("/api/v1/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskId.toString())))
                .andExpect(jsonPath("$.title", is("Test Task Name")))
                .andExpect(jsonPath("$.status", is("in_progress")));
    }

    @Test
    public void testRejectTransitionToDoneWhenPrClosedWithoutMerge() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Status Reconciliation Task", "in_progress", 53, "open", false);
        taskRepository.saveAndFlush(task);

        // Register GitHub truth for PR 53: closed and NOT merged
        gitHubService.registerPrStatus(53, "closed", false);

        // Attempting to transition to 'done' should be rejected
        mockMvc.perform(put("/api/v1/tasks/" + taskId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "done"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("CONSTRAINT_VIOLATION")))
                .andExpect(jsonPath("$.message", containsString("closed without being merged")));

        // Verify task status remains 'in_progress'
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("in_progress", reloaded.getStatus());
    }

    @Test
    public void testAllowTransitionToDoneWhenPrMerged() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "E2E Sync Task", "in_progress", 53, "open", false);
        taskRepository.saveAndFlush(task);

        // Register GitHub truth for PR 53: closed and MERGED
        gitHubService.registerPrStatus(53, "closed", true);

        // Attempting to transition to 'done' should succeed
        mockMvc.perform(put("/api/v1/tasks/" + taskId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "done"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("done")));

        // Verify task status updated to 'done'
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("done", reloaded.getStatus());
    }

    @Test
    public void testReconciliationCorrectsMismatchStatusToFailed() throws Exception {
        UUID taskId = UUID.fromString("0bcb9d29-ad04-4c30-8448-e3cbacf70c4f");
        // Task starts at 'done' internally
        Task task = new Task(taskId, "Mismatched Task", "done", 53, "closed", false);
        taskRepository.saveAndFlush(task);

        // Register GitHub truth: closed and unmerged
        gitHubService.registerPrStatus(53, "closed", false);

        // Trigger reconciliation via endpoint
        mockMvc.perform(post("/api/v1/tasks/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.reconciledCount", is(1)));

        // Verify that the task status has been corrected to 'failed'
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus());
    }

    @Test
    public void testReconciliationDoesNothingWhenNoMismatch() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Correct Task", "done", 53, "closed", true);
        taskRepository.saveAndFlush(task);

        // Register GitHub truth: closed and merged
        gitHubService.registerPrStatus(53, "closed", true);

        // Trigger reconciliation
        mockMvc.perform(post("/api/v1/tasks/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciledCount", is(0)));

        // Verify status remains 'done'
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("done", reloaded.getStatus());
    }

    @Test
    public void testAtomicUpdateStatusQueryDirectly() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Atomic test", "done", 99, "closed", false);
        taskRepository.saveAndFlush(task);

        // Try updating with mismatched expected status -> should update 0 rows
        int rowsUpdated = taskRepository.updateStatusAtomically(taskId, "failed", "in_progress");
        assertEquals(0, rowsUpdated);

        // Verify task is still 'done'
        assertEquals("done", taskRepository.findById(taskId).orElseThrow().getStatus());

        // Update with matching expected status -> should update 1 row
        rowsUpdated = taskRepository.updateStatusAtomically(taskId, "failed", "done");
        assertEquals(1, rowsUpdated);

        // Verify task is now 'failed'
        assertEquals("failed", taskRepository.findById(taskId).orElseThrow().getStatus());
    }

    @Test
    public void testScheduledSyncBypassesDoneStatusWhenPrClosedAndUnmerged() {
        // Reset spy invocation count
        reset(taskRepository);

        UUID taskId = UUID.randomUUID();
        // Task starts at 'in_progress' internally
        Task task = new Task(taskId, "Closed But Unmerged Task", "in_progress", 88, "closed", false);
        taskRepository.saveAndFlush(task);

        // Register GitHub truth: closed and UNMERGED
        gitHubService.registerPrStatus(88, "closed", false);

        // Trigger synchronization via scheduler
        taskSyncScheduler.runSyncJob();

        // Verify that the task status was updated to 'in_progress' (retaining active status) rather than 'failed' or 'done'
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("in_progress", reloaded.getStatus());

        // Verify that updateStatusAndPrStateAtomically was indeed called with "in_progress" status for this task
        verify(taskRepository, times(1)).updateStatusAndPrStateAtomically(eq(taskId), eq("in_progress"), eq("in_progress"), eq("closed"), eq(false), any());
    }

    @Test
    public void testScheduledSyncUpdatesToDoneWhenPrClosedAndMerged() {
        // Reset spy invocation count
        reset(taskRepository);

        UUID taskId = UUID.randomUUID();
        // Task starts at 'in_progress' internally
        Task task = new Task(taskId, "Closed and Merged Task", "in_progress", 89, "closed", true);
        taskRepository.saveAndFlush(task);

        // Register GitHub truth: closed and MERGED
        gitHubService.registerPrStatus(89, "closed", true);

        // Trigger synchronization via scheduler
        taskSyncScheduler.runSyncJob();

        // Verify that the task status was updated to 'done'
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("done", reloaded.getStatus());

        // Verify that updateStatusAndPrStateAtomically was indeed called with "done" status for this task
        verify(taskRepository, times(1)).updateStatusAndPrStateAtomically(eq(taskId), eq("done"), eq("in_progress"), eq("closed"), eq(true), any());
    }

    @Test
    public void testTaskClosedAndUnmergedVerifyInternalStatusNotDoneAndPrStateMapped() throws Exception {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task to close", "in_progress", 777, "open", false);
        taskRepository.saveAndFlush(task);

        // Register PR 777 as closed and unmerged
        gitHubService.registerPrStatus(777, "closed", false);

        // Run sync job
        taskSyncScheduler.runSyncJob();

        // Reload the task
        Task reloaded = taskRepository.findById(taskId).orElseThrow();

        // Internal status is in_progress, NOT done
        assertNotEquals("done", reloaded.getStatus());
        assertEquals("in_progress", reloaded.getStatus());

        // Internal PR fields are updated correctly
        assertEquals("closed", reloaded.getGithubPrState());
        assertEquals(false, reloaded.getGithubPrMerged());
    }

    @Test
    public void testFlowCoreBlockedWhenFailedTasksExist() throws Exception {
        // Assert initial flow state is ACTIVE
        mockMvc.perform(get("/api/v1/tasks/flow-core/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("ACTIVE")))
                .andExpect(jsonPath("$.failedTasksCount", is(0)));

        // Create a failed task
        UUID taskId = UUID.randomUUID();
        Task failedTask = new Task(taskId, "Failed Task", "failed", 101, "closed", false);
        taskRepository.saveAndFlush(failedTask);

        // Assert flow state is now BLOCKED_BY_FAILED_FRONTIER
        mockMvc.perform(get("/api/v1/tasks/flow-core/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("BLOCKED_BY_FAILED_FRONTIER")))
                .andExpect(jsonPath("$.failedTasksCount", is(1)));
    }

    @Test
    public void testFlowCoreUnblockTransitionsFailedTasksToOpen() throws Exception {
        // Create 5 failed tasks (representing the five failed tasks of the brief)
        for (int i = 0; i < 5; i++) {
            UUID taskId = UUID.randomUUID();
            Task failedTask = new Task(taskId, "Failed Task " + i, "failed", 200 + i, "closed", false);
            taskRepository.saveAndFlush(failedTask);
            // Register GitHub truth for the PR: closed and NOT merged
            gitHubService.registerPrStatus(200 + i, "closed", false);
        }

        // Verify state is blocked with 5 failed tasks
        mockMvc.perform(get("/api/v1/tasks/flow-core/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("BLOCKED_BY_FAILED_FRONTIER")))
                .andExpect(jsonPath("$.failedTasksCount", is(5)));

        // Execute the unblocking patch
        mockMvc.perform(post("/api/v1/tasks/flow-core/unblock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetStatus", "open"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.unblockedCount", is(5)));

        // Verify flow core is active now
        mockMvc.perform(get("/api/v1/tasks/flow-core/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("ACTIVE")))
                .andExpect(jsonPath("$.failedTasksCount", is(0)));

        // Trigger synchronization via scheduler to make sure they do not revert to failed
        taskSyncScheduler.runSyncJob();

        // Verify tasks statuses remained 'open' and github PR details are cleared/dissociated
        List<Task> tasks = taskRepository.findAll();
        assertFalse(tasks.isEmpty());
        for (Task t : tasks) {
            assertEquals("open", t.getStatus());
            assertNull(t.getGithubPrNumber());
            assertNull(t.getGithubPrState());
            assertNull(t.getGithubPrMerged());
        }

        // Verify state is still ACTIVE with 0 failed tasks
        mockMvc.perform(get("/api/v1/tasks/flow-core/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("ACTIVE")))
                .andExpect(jsonPath("$.failedTasksCount", is(0)));
    }

    @Test
    public void testFlowCoreUnblockTransitionsFailedTasksWithCustomTargetStatus() throws Exception {
        // Create failed tasks with a specific custom target status transition in mind
        for (int i = 0; i < 3; i++) {
            UUID taskId = UUID.randomUUID();
            Task failedTask = new Task(taskId, "Custom Unblock Failed Task " + i, "failed", 300 + i, "closed", false);
            taskRepository.saveAndFlush(failedTask);
            gitHubService.registerPrStatus(300 + i, "closed", false);
        }

        // Verify initial state is blocked
        mockMvc.perform(get("/api/v1/tasks/flow-core/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("BLOCKED_BY_FAILED_FRONTIER")))
                .andExpect(jsonPath("$.failedTasksCount", is(3)));

        // Execute the unblocking patch with custom targetStatus: "in_progress"
        mockMvc.perform(post("/api/v1/tasks/flow-core/unblock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetStatus", "in_progress"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.unblockedCount", is(3)));

        // Verify flow core is active now
        mockMvc.perform(get("/api/v1/tasks/flow-core/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("ACTIVE")))
                .andExpect(jsonPath("$.failedTasksCount", is(0)));

        // Verify tasks statuses became 'in_progress' and github PR details are cleared/dissociated
        List<Task> tasks = taskRepository.findAll();
        assertFalse(tasks.isEmpty());
        for (Task t : tasks) {
            assertEquals("in_progress", t.getStatus());
            assertNull(t.getGithubPrNumber());
            assertNull(t.getGithubPrState());
            assertNull(t.getGithubPrMerged());
        }
    }
}
