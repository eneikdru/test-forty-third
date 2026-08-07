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
    public void testReconciliationCorrectsMismatchStatusToUnmerged() throws Exception {
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

        // Verify that the task status has been corrected to 'unmerged'
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("unmerged", reloaded.getStatus());
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
        int rowsUpdated = taskRepository.updateStatusAtomically(taskId, "unmerged", "in_progress");
        assertEquals(0, rowsUpdated);

        // Verify task is still 'done'
        assertEquals("done", taskRepository.findById(taskId).orElseThrow().getStatus());

        // Update with matching expected status -> should update 1 row
        rowsUpdated = taskRepository.updateStatusAtomically(taskId, "unmerged", "done");
        assertEquals(1, rowsUpdated);

        // Verify task is now 'unmerged'
        assertEquals("unmerged", taskRepository.findById(taskId).orElseThrow().getStatus());
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

        // Verify that the task status was NOT updated to 'done' (remains 'in_progress')
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("in_progress", reloaded.getStatus());

        // Verify that updateStatusAtomically was never called with "done" status for this task
        verify(taskRepository, never()).updateStatusAtomically(eq(taskId), eq("done"), anyString());
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

        // Verify that updateStatusAtomically was indeed called with "done" status for this task
        verify(taskRepository, times(1)).updateStatusAtomically(eq(taskId), eq("done"), eq("in_progress"));
    }
}
