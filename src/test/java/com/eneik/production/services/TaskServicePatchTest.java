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
        assertEquals("failed", updated.getStatus());
    }
}
