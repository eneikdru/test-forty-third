package com.eneik.production.services;

import com.eneik.generated.model.Task;
import com.eneik.generated.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PipelineTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(PipelineTelemetryService.class);

    private final TaskRepository taskRepository;
    private final JulesApiClient julesApiClient;
    private boolean payloadIssuePersisting = false;

    public PipelineTelemetryService(TaskRepository taskRepository, JulesApiClient julesApiClient) {
        this.taskRepository = taskRepository;
        this.julesApiClient = julesApiClient;
    }

    public boolean isPayloadIssuePersisting() {
        return payloadIssuePersisting;
    }

    public void setPayloadIssuePersisting(boolean payloadIssuePersisting) {
        this.payloadIssuePersisting = payloadIssuePersisting;
    }

    /**
     * Scans tasks stuck in the 'implementing' state.
     * If the payload limit issue persists (as indicated by telemetry),
     * automatically and atomically reverts their status to 'failed'.
     */
    public int checkAndRevertStuckPipelines() {
        if (!payloadIssuePersisting) {
            log.info("[TELEMETRY] Pipeline telemetry check: No active payload issues persisting. No reverts needed.");
            return 0;
        }

        log.warn("[TELEMETRY][FLOW_RECOVERY] Payload limit issue persists according to telemetry. Scanning for stuck tasks in IMPLEMENTING...");

        List<Task> tasks = taskRepository.findAll();
        int revertedCount = 0;

        for (Task task : tasks) {
            String currentStatus = task.getStatus();
            if ("implementing".equalsIgnoreCase(currentStatus) || "IMPLEMENTING".equalsIgnoreCase(currentStatus)) {
                log.warn("[TELEMETRY][REVERT] Pipeline stuck in IMPLEMENTING state detected for task {}. Reverting due to persistent 10MB payload limit issue...", task.getId());

                // Atomically update the task's status to 'failed' to prevent concurrent state corruption
                int updatedRows = taskRepository.updateStatusAtomically(task.getId(), "failed", currentStatus);
                if (updatedRows > 0) {
                    revertedCount++;
                    log.info("[TELEMETRY][REVERT_COMPLETE] Successfully reverted task {} status from {} to failed.", task.getId(), currentStatus);
                }
            }
        }

        return revertedCount;
    }
}
