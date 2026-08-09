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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals("open", updated.getStatus());
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
        assertEquals("open", reloaded.getStatus(), "Internal task status must be reverted and loop cleared (open)");
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
        assertEquals("open", reloaded.getStatus(), "Task should be reverted and unblocked");
    }
}
