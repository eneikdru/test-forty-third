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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class, properties = {"spring.main.allow-bean-definition-overriding=true"})
@ActiveProfiles("test")
@Transactional
public class PrRejectionLogicTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private GitHubService gitHubService;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        gitHubService.clearRegistry();
    }

    @Test
    public void testRejectionLogicTransitionToDoneFailsWhenPrIsClosedAndUnmerged() {
        // Given a task associated with PR 9001, which is in_progress
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Rejection Test Task 1", "in_progress", 9001, "open", false);
        taskRepository.saveAndFlush(task);

        // And the GitHub PR is closed and unmerged (rejection logic trigger)
        gitHubService.registerPrStatus(9001, "closed", false);

        // When/Then attempting to set the status to 'done' must trigger IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            taskService.updateTaskStatus(taskId, "done");
        });

        assertTrue(ex.getMessage().contains("is closed without being merged"), "Expected closed unmerged rejection message");

        // Confirm task status was not changed
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("in_progress", reloaded.getStatus());
    }

    @Test
    public void testRejectionLogicTransitionToDoneFailsWhenPrIsOpenAndUnmerged() {
        // Given a task associated with PR 9002, which is in_progress
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Rejection Test Task 2", "in_progress", 9002, "open", false);
        taskRepository.saveAndFlush(task);

        // And the GitHub PR is open and unmerged
        gitHubService.registerPrStatus(9002, "open", false);

        // When/Then attempting to set the status to 'done' must trigger IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            taskService.updateTaskStatus(taskId, "done");
        });

        assertTrue(ex.getMessage().contains("is not merged"), "Expected open unmerged rejection message");

        // Confirm task status was not changed
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("in_progress", reloaded.getStatus());
    }

    @Test
    public void testRejectionLogicTransitionToDoneSucceedsWhenPrIsClosedAndMerged() {
        // Given a task associated with PR 9003, which is in_progress
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Rejection Test Task 3", "in_progress", 9003, "open", false);
        taskRepository.saveAndFlush(task);

        // And the GitHub PR is closed and merged
        gitHubService.registerPrStatus(9003, "closed", true);

        // When transitioning to 'done'
        assertDoesNotThrow(() -> {
            taskService.updateTaskStatus(taskId, "done");
        });

        // Then the status is successfully updated to 'done'
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("done", reloaded.getStatus());
    }
}
