package com.eneik.production.services;

import com.eneik.generated.Application;
import com.eneik.generated.model.Task;
import com.eneik.generated.repository.TaskRepository;
import com.eneik.generated.service.GitHubService;
import com.eneik.generated.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Application.class, properties = {"spring.main.allow-bean-definition-overriding=true"})
@ActiveProfiles("test")
@Transactional
public class TaskServicePatchTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private GitHubService gitHubService;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    public void testPatchIsLoaded() {
        assertTrue(taskService instanceof TaskServicePatch, "TaskServicePatch should be loaded as Primary bean");
    }

    @Test
    public void testUpdateTaskStatusIsAtomic() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Test Task", "in_progress", 123, "open", false);
        taskRepository.saveAndFlush(task);

        taskService.updateTaskStatus(taskId, "open");

        Task updated = taskRepository.findById(taskId).orElseThrow();
        assertEquals("open", updated.getStatus());
    }

    @Test
    public void testSyncTaskStatusesWithGitHubUsesCorrectOldStatus() {
        UUID taskId = UUID.randomUUID();
        // Task is marked 'done' locally but GitHub says closed without merge
        Task task = new Task(taskId, "Mismatched Task", "done", 124, "closed", false);
        taskRepository.saveAndFlush(task);

        gitHubService.registerPrStatus(124, "closed", false);

        taskService.syncTaskStatusesWithGitHub();

        Task updated = taskRepository.findById(taskId).orElseThrow();
        // Should correctly transition from 'done' to 'failed'
        assertEquals("failed", updated.getStatus());
    }

    @Test
    public void testDoneTaskWithClosedUnmergedPrRevertsToFailedState() {
        UUID taskId = UUID.randomUUID();
        // Given a task with an internal status of 'done' and an associated GitHub PR (number 555)
        Task task = new Task(taskId, "Done Task With Unmerged PR", "done", 555, "open", false);
        taskRepository.saveAndFlush(task);

        // When the system checks the PR state and finds it closed but not merged
        gitHubService.registerPrStatus(555, "closed", false);
        taskService.syncTaskStatusesWithGitHub();

        // Then the internal task status must not be 'done' and must be reverted to reflect the unmerged PR state (failed)
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus(), "Internal task status must be reverted to reflect the unmerged PR state (failed)");
    }

    @Test
    public void testTransitionToDoneFailsWhenPrIsOpenAndUnmerged() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task With Open PR", "in_progress", 777, "open", false);
        taskRepository.saveAndFlush(task);

        // GitHub PR is open and unmerged
        gitHubService.registerPrStatus(777, "open", false);

        try {
            taskService.updateTaskStatus(taskId, "done");
            org.junit.jupiter.api.Assertions.fail("Should have thrown IllegalStateException because PR is open and unmerged");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("is not merged"), "Expected exception message to contain 'is not merged'");
        }

        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("in_progress", reloaded.getStatus());
    }

    @Test
    public void testReconciliationRevertsDoneTaskToFailedWhenPrIsOpenAndUnmerged() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Done Task with Open PR", "done", 888, "open", false);
        taskRepository.saveAndFlush(task);

        // GitHub PR is open and unmerged
        gitHubService.registerPrStatus(888, "open", false);

        taskService.syncTaskStatusesWithGitHub();

        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus(), "Task should be reverted and unblocked");
    }

    @Test
    public void testSyncTaskStatusesDoesNotPerformRedundantUpdatesOnSubsequentRuns() {
        UUID taskId = UUID.randomUUID();
        // Given a task that is 'done' internally
        Task task = new Task(taskId, "Test Redundant Update Task", "done", 1001, "open", false);
        taskRepository.saveAndFlush(task);

        // And the GitHub PR is closed and unmerged
        gitHubService.registerPrStatus(1001, "closed", false);

        // First synchronization: should revert task status to 'failed' and reconcile
        int firstRunCount = taskService.syncTaskStatusesWithGitHub();
        assertEquals(1, firstRunCount, "First synchronization run should reconcile the mismatched task");

        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus(), "Task should be reverted to failed status");
        assertEquals("closed", reloaded.getGithubPrState(), "PR state should be updated to closed");
        assertEquals(false, reloaded.getGithubPrMerged(), "PR merged should be false");

        // Second synchronization: should do nothing (reconciledCount = 0) since everything already matches
        int secondRunCount = taskService.syncTaskStatusesWithGitHub();
        assertEquals(0, secondRunCount, "Second synchronization run should skip already reconciled task");
    }

    @Test
    public void testSyncTaskStatusesFromInProgressDoesNotPerformRedundantUpdates() {
        UUID taskId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        // Given an in_progress task internally
        Task task = new Task(taskId, "Test Redundant Active Task", "in_progress", 1002, "open", false);
        taskRepository.saveAndFlush(task);

        // And the GitHub PR is closed and unmerged
        gitHubService.registerPrStatus(1002, "closed", false);

        // First synchronization: should keep status unchanged ('in_progress') and reconcile metadata
        int firstRunCount = taskService.syncTaskStatusesWithGitHub();
        assertEquals(1, firstRunCount, "First synchronization run should reconcile the active task");

        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("in_progress", reloaded.getStatus(), "Active task with closed unmerged PR should remain unchanged");
        assertEquals("closed", reloaded.getGithubPrState(), "PR state should be updated to closed");
        assertEquals(false, reloaded.getGithubPrMerged(), "PR merged should be false");

        // Second synchronization: should do nothing (reconciledCount = 0) since everything already matches
        int secondRunCount = taskService.syncTaskStatusesWithGitHub();
        assertEquals(0, secondRunCount, "Second synchronization run should skip already reconciled active task");
    }

    @Test
    public void testSyncTaskStatusesFromDoneToFailedOnUnmergedClosedPrWithNoRedundantUpdates() {
        UUID taskId = UUID.fromString("99999999-8888-7777-6666-555555555555");
        // Given a task that is 'done' internally
        Task task = new Task(taskId, "Test Redundant Done Task", "done", 1003, "open", false);
        taskRepository.saveAndFlush(task);

        // And the GitHub PR is closed and unmerged
        gitHubService.registerPrStatus(1003, "closed", false);

        // First synchronization: should revert task status to 'failed' and reconcile
        int firstRunCount = taskService.syncTaskStatusesWithGitHub();
        assertEquals(1, firstRunCount, "First synchronization run should reconcile the mismatched task");

        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus(), "Done task should be reverted to failed status");
        assertEquals("closed", reloaded.getGithubPrState(), "PR state should be updated to closed");
        assertEquals(false, reloaded.getGithubPrMerged(), "PR merged should be false");

        // Second synchronization: should do nothing (reconciledCount = 0) since everything already matches
        int secondRunCount = taskService.syncTaskStatusesWithGitHub();
        assertEquals(0, secondRunCount, "Second synchronization run should skip already reconciled task");
    }

    @Test
    public void testClosedUnmergedPrSetsStatusToFailed() {
        UUID taskId = UUID.randomUUID();
        // Given a task that is 'done' internally
        Task task = new Task(taskId, "Task closed without merge", "done", 601, "open", false);
        taskRepository.saveAndFlush(task);

        // When its associated PR is closed without merge
        gitHubService.registerPrStatus(601, "closed", false);
        taskService.syncTaskStatusesWithGitHub();

        // Then the internal status correctly reflects the closed/failed state
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus());
    }

    @Test
    public void testSubsequentSyncRunsDoNotEmitWarnLogsForTask0bcb9d29() {
        UUID taskId = UUID.fromString("0bcb9d29-ad04-4c30-8448-e3cbacf70c4f");
        // Given task 0bcb9d29 is 'done' internally with a closed unmerged PR
        Task task = new Task(taskId, "Task 0bcb9d29 Status Test", "done", 53, "open", false);
        taskRepository.saveAndFlush(task);
        gitHubService.registerPrStatus(53, "closed", false);

        // Setup Logback ListAppender to intercept logs
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(TaskService.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            // First synchronization run: corrects the mismatch from 'done' to 'failed' and may log a warn
            taskService.syncTaskStatusesWithGitHub();

            Task reloaded = taskRepository.findById(taskId).orElseThrow();
            assertEquals("failed", reloaded.getStatus());

            // Clear intercepted logs from the first run
            listAppender.list.clear();

            // When the synchronization test checks/runs again
            taskService.syncTaskStatusesWithGitHub();

            // Then no WARN logs for task 0bcb9d29 are emitted on the second run (confirmed eliminated)
            boolean warnLoggedOnSubsequent = listAppender.list.stream()
                    .filter(event -> event.getLevel().equals(ch.qos.logback.classic.Level.WARN))
                    .anyMatch(event -> event.getFormattedMessage().contains("0bcb9d29"));

            assertFalse(warnLoggedOnSubsequent, "No WARN logs should be emitted for task 0bcb9d29 on subsequent synchronization runs");
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    public void testActiveTaskClosedUnmergedPrRemainsUnchanged() {
        UUID taskId = UUID.randomUUID();
        // Given an active task with 'in_progress' status
        Task task = new Task(taskId, "Active In-Progress Task", "in_progress", 9999, "open", false);
        taskRepository.saveAndFlush(task);

        // When the associated GitHub PR is closed and unmerged (false)
        gitHubService.registerPrStatus(9999, "closed", false);
        taskService.syncTaskStatusesWithGitHub();

        // Then the internal status of the active task must remain unchanged ('in_progress')
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("in_progress", reloaded.getStatus(), "Internal task status must remain unchanged");
        assertEquals("closed", reloaded.getGithubPrState());
        assertFalse(reloaded.getGithubPrMerged());
    }
}
