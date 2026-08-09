package com.eneik.production.services;

import com.eneik.generated.model.Task;
import com.eneik.generated.repository.TaskRepository;
import com.eneik.generated.service.GitHubService;
import com.eneik.generated.service.TaskService;
import com.eneik.generated.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Primary
@Service
@Transactional
public class TaskServicePatch extends TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final GitHubService gitHubService;
    private final TimeProvider timeProvider;

    public TaskServicePatch(TaskRepository taskRepository, GitHubService gitHubService, TimeProvider timeProvider) {
        super(taskRepository, gitHubService);
        this.taskRepository = taskRepository;
        this.gitHubService = gitHubService;
        this.timeProvider = timeProvider;
    }

    @Override
    public Task updateTaskStatus(UUID id, String targetStatus) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));

        if ("done".equalsIgnoreCase(targetStatus) && task.getGithubPrNumber() != null) {
            GitHubService.PrStatus prStatus = gitHubService.getPrStatus(task.getGithubPrNumber());
            if (!prStatus.isMerged()) {
                if ("closed".equalsIgnoreCase(prStatus.getState())) {
                    throw new IllegalStateException(
                            "Cannot transition task status to 'done' because associated PR #"
                            + task.getGithubPrNumber() + " is closed without being merged."
                    );
                } else {
                    throw new IllegalStateException(
                            "Cannot transition task status to 'done' because associated PR #"
                            + task.getGithubPrNumber() + " is not merged."
                    );
                }
            }
        }

        int updated = taskRepository.updateStatusAtomically(id, targetStatus, task.getStatus());
        if (updated == 0) {
            throw new IllegalStateException("Task status was modified concurrently or concurrent state change");
        }
        return taskRepository.findById(id).orElseThrow();
    }

    /**
     * Reverts the task status to the appropriate unmerged PR state.
     * When a task is marked 'done' internally but its associated GitHub PR is closed and unmerged,
     * its status is reverted to 'failed' to reflect the unmerged PR state and halt/reset the Flow Core.
     */
    private boolean revertToUnmergedPrState(Task task, GitHubService.PrStatus prStatus) {
        if ("closed".equalsIgnoreCase(prStatus.getState())) {
            log.warn("[TELEMETRY][TASK_RECONCILIATION] syncTaskStatusesWithGitHub: task {} is marked done but PR#{} closed without merge",
                    task.getId(), task.getGithubPrNumber());
        } else {
            log.warn("[TELEMETRY][TASK_RECONCILIATION] syncTaskStatusesWithGitHub: task {} is marked done but PR#{} is not merged",
                    task.getId(), task.getGithubPrNumber());
        }

        // Set status to failed and store the actual GitHub PR state and merged status to avoid redundant DB updates
        int updatedRows = taskRepository.updateStatusAndPrStateAtomically(
                task.getId(), "failed", task.getStatus(), prStatus.getState(), prStatus.isMerged(), timeProvider.now()
        );
        return updatedRows > 0;
    }

    /**
     * Helper to deterministically calculate the correct target state of a task
     * based on its current status and its corresponding GitHub PR state.
     * Under the semantic contextualism framework of BARCAN-TAG-02, names of statuses
     * such as 'done' and 'failed' act as rigid designators with stable meanings.
     * Core Fix for Findings 6 & 7: Any task with a closed and unmerged PR
     * must transition to 'failed' status to halt/reset the Flow Core.
     */
    private String determineTargetStatus(String currentStatus, String prState, boolean isMerged) {
        if (prState == null) {
            return currentStatus;
        }
        if ("closed".equalsIgnoreCase(prState)) {
            return isMerged ? "done" : "failed";
        }
        // If PR is open but task is internally 'done', revert to failed.
        if ("done".equalsIgnoreCase(currentStatus)) {
            return "failed";
        }
        return currentStatus;
    }

    @Override
    public int syncTaskStatusesWithGitHub() {
        List<Task> tasks = taskRepository.findAll();
        int reconciledCount = 0;

        for (Task task : tasks) {
            if (task.getGithubPrNumber() == null) {
                continue;
            }

            GitHubService.PrStatus prStatus = gitHubService.getPrStatus(task.getGithubPrNumber());
            String prState = prStatus.getState();
            boolean isMerged = prStatus.isMerged();

            // Calculate the target status using the refactored, robust, deterministic state machine
            String targetStatus = determineTargetStatus(task.getStatus(), prState, isMerged);

            // Redundant database updates on subsequent scheduler runs are prevented by checking
            // if any state fields (status, state, merged flag) actually differ from the current DB truth.
            if (!targetStatus.equalsIgnoreCase(task.getStatus())
                    || !prState.equalsIgnoreCase(task.getGithubPrState())
                    || task.getGithubPrMerged() == null
                    || task.getGithubPrMerged() != isMerged) {

                if ("failed".equalsIgnoreCase(targetStatus) && !targetStatus.equalsIgnoreCase(task.getStatus())) {
                    log.warn("[TELEMETRY][TASK_RECONCILIATION] syncTaskStatusesWithGitHub: task {} transitioning from {} to failed due to unmerged PR#{}",
                            task.getId(), task.getStatus(), task.getGithubPrNumber());
                } else {
                    log.info("syncTaskStatusesWithGitHub (patched): task {} (current status: {}) transitioning to target status: {}, PR state: {}, merged: {}",
                            task.getId(), task.getStatus(), targetStatus, prState, isMerged);
                }

                // Explicitly transition task status to targetStatus (never retaining the incorrect 'done' status)
                String nextStatus = targetStatus;
                int updatedRows = taskRepository.updateStatusAndPrStateAtomically(
                        task.getId(), nextStatus, task.getStatus(), prState, isMerged, timeProvider.now()
                );
                if (updatedRows > 0) {
                    reconciledCount++;
                }
            }
        }

        return reconciledCount;
    }
}
