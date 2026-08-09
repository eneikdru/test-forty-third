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
public class JulesTelemetryIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private JulesApiClient julesApiClient;

    @Autowired
    private PipelineTelemetryService pipelineTelemetryService;

    @Autowired
    private GitHubService gitHubService;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        julesApiClient.reset();
        pipelineTelemetryService.setPayloadIssuePersisting(false);
    }

    @Test
    public void testJulesApiClientHandlesLargePayloadWithoutSkippingScan() {
        // Given a response payload exceeding 10MB (10 * 1024 * 1024 bytes) in JulesApiClient
        int sizeOf10MBPlus = 10 * 1024 * 1024 + 1024; // 10MB + 1KB
        byte[] largePayload = new byte[sizeOf10MBPlus];

        // When it is processed
        julesApiClient.processPayload("session_7800182329015729496", largePayload);

        // Then the client must handle it without skipping activity scans
        assertTrue(julesApiClient.isActivityScanned(), "Activity scan should have run and not been skipped");
        assertTrue(julesApiClient.isMitigatedExceededPayload(), "The payload limit constraint should be mitigated");
    }

    @Test
    public void testJulesApiClientHandlesNormalPayloadNormally() {
        // Given a response payload within normal limit
        byte[] normalPayload = new byte[1024]; // 1KB

        // When it is processed
        julesApiClient.processPayload("session_12345", normalPayload);

        // Then it processes normally
        assertTrue(julesApiClient.isActivityScanned(), "Activity scan should have run");
        assertFalse(julesApiClient.isMitigatedExceededPayload(), "Payload limit constraint should not be triggered");
    }

    @Test
    public void testPipelineStuckInImplementingRevertsWhenIssuePersists() {
        // Given a pipeline stuck in the IMPLEMENTING state
        UUID taskId = UUID.randomUUID();
        Task stuckTask = new Task(taskId, "Stuck Feature Implementation", "implementing", 999, "open", false);
        taskRepository.saveAndFlush(stuckTask);

        // When telemetry indicates the 10MB payload limit issue persists
        pipelineTelemetryService.setPayloadIssuePersisting(true);

        // When synchronization (which runs telemetry-driven reverts) is executed
        taskService.syncTaskStatusesWithGitHub();

        // Then PRs/tasks claiming to fix it without symptom resolution must be automatically reverted to failed
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus(), "Task stuck in implementing should be automatically reverted to failed");
    }

    @Test
    public void testPipelineStuckInImplementingCaseInsensitiveReverts() {
        // Given a pipeline stuck in the IMPLEMENTING state (with uppercase status)
        UUID taskId = UUID.randomUUID();
        Task stuckTask = new Task(taskId, "Stuck Feature Implementation", "IMPLEMENTING", 999, "open", false);
        taskRepository.saveAndFlush(stuckTask);

        // When telemetry indicates the 10MB payload limit issue persists
        pipelineTelemetryService.setPayloadIssuePersisting(true);

        // When synchronization is executed
        taskService.syncTaskStatusesWithGitHub();

        // Then it is automatically reverted to failed
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus(), "Task stuck in IMPLEMENTING should be automatically reverted to failed");
    }

    @Test
    public void testPipelineImplementingDoesNotRevertWhenIssueIsResolved() {
        // Given a pipeline stuck in the IMPLEMENTING state
        UUID taskId = UUID.randomUUID();
        Task stuckTask = new Task(taskId, "Stuck Feature Implementation", "implementing", 999, "open", false);
        taskRepository.saveAndFlush(stuckTask);

        // When telemetry indicates the issue is resolved (not persisting)
        pipelineTelemetryService.setPayloadIssuePersisting(false);

        // When synchronization is executed
        taskService.syncTaskStatusesWithGitHub();

        // Then the task status remains unchanged
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("implementing", reloaded.getStatus(), "Task status should remain unchanged when the issue does not persist");
    }
}
