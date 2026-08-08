package com.eneik.generated.service;

import com.eneik.generated.model.Task;
import com.eneik.generated.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final GitHubService gitHubService;

    public TaskService(TaskRepository taskRepository, GitHubService gitHubService) {
        this.taskRepository = taskRepository;
        this.gitHubService = gitHubService;
    }

    public Task createTask(Task task) {
        if (task.getId() == null) {
            task.setId(UUID.randomUUID());
        }
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    /**
     * Retrieves the current state of the Flow Core.
     */
    public Map<String, Object> getFlowCoreState() {
        long failedTasksCount = taskRepository.countByStatus("failed");
        String state = failedTasksCount > 0 ? "BLOCKED_BY_FAILED_FRONTIER" : "ACTIVE";
        return Map.of(
            "state", state,
            "failedTasksCount", failedTasksCount
        );
    }

    /**
     * Unblocks the Flow Core by transitioning failed tasks to an active state.
     */
    public int unblockFlowCore(String targetStatus) {
        String resolvedTarget = (targetStatus == null || targetStatus.trim().isEmpty()) ? "open" : targetStatus;
        log.info("unblockFlowCore: Transitioning all failed tasks to '{}' to unblock Flow Core", resolvedTarget);
        int updated = taskRepository.updateAllStatusAtomically("failed", resolvedTarget, LocalDateTime.now());
        log.info("unblockFlowCore: Successfully transitioned {} failed tasks to '{}'", updated, resolvedTarget);
        return updated;
    }

    public Optional<Task> getTask(UUID id) {
        return taskRepository.findById(id);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    /**
     * Transitions a task's status with validation against GitHub truth.
     */
    public Task updateTaskStatus(UUID id, String targetStatus) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));

        if ("done".equalsIgnoreCase(targetStatus) && task.getGithubPrNumber() != null) {
            GitHubService.PrStatus prStatus = gitHubService.getPrStatus(task.getGithubPrNumber());
            if ("closed".equalsIgnoreCase(prStatus.getState()) && !prStatus.isMerged()) {
                throw new IllegalStateException(
                        "Cannot transition task status to 'done' because associated PR #"
                        + task.getGithubPrNumber() + " is closed without being merged."
                );
            }
        }

        task.setStatus(targetStatus);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    /**
     * Reconciles all tasks status against GitHub reality.
     * If a task is marked 'done' internally but the associated GitHub PR is closed and unmerged,
     * updates the state to 'failed' using an atomically-guarded query.
     */
    public int reconcileTaskStatusAgainstGitHubTruth() {
        return syncTaskStatusesWithGitHub();
    }

    /**
     * Syncs tasks status with GitHub reality.
     * If a task is marked 'done' but its associated GitHub PR is closed and unmerged, transitions status to 'failed'.
     * If a task is not marked 'done' but its associated GitHub PR is closed and merged, transitions status to 'done'.
     * If a task is not marked 'done' and its associated GitHub PR is closed and unmerged, transitions status to 'failed'.
     * All database updates use an atomically-guarded query.
     */
    public int syncTaskStatusesWithGitHub() {
        List<Task> tasks = taskRepository.findAll();
        int reconciledCount = 0;

        for (Task task : tasks) {
            if (task.getGithubPrNumber() == null) {
                continue;
            }

            GitHubService.PrStatus prStatus = gitHubService.getPrStatus(task.getGithubPrNumber());

            if ("done".equalsIgnoreCase(task.getStatus())) {
                // Task is done but PR is closed and unmerged -> update status to failed
                if ("closed".equalsIgnoreCase(prStatus.getState()) && !prStatus.isMerged()) {
                    log.warn("[TELEMETRY][TASK_RECONCILIATION] task {} is marked done but PR#{} closed without merge",
                            task.getId(), task.getGithubPrNumber());

                    int updatedRows = taskRepository.updateStatusAndPrStateAtomically(
                            task.getId(), "failed", "done", prStatus.getState(), prStatus.isMerged(), LocalDateTime.now()
                    );
                    if (updatedRows > 0) {
                        reconciledCount++;
                    }
                }
            } else {
                // Task is not done
                if ("closed".equalsIgnoreCase(prStatus.getState())) {
                    if (prStatus.isMerged()) {
                        // PR is closed and merged -> update task status to done
                        log.info("syncTaskStatusesWithGitHub: task {} has merged PR#{}, transitioning status to done",
                                task.getId(), task.getGithubPrNumber());

                        int updatedRows = taskRepository.updateStatusAndPrStateAtomically(
                                task.getId(), "done", task.getStatus(), prStatus.getState(), prStatus.isMerged(), LocalDateTime.now()
                        );
                        if (updatedRows > 0) {
                            reconciledCount++;
                        }
                    } else {
                        // PR is closed and unmerged -> update task status to failed
                        log.info("syncTaskStatusesWithGitHub: task {} has unmerged closed PR#{}, transitioning status to failed",
                                task.getId(), task.getGithubPrNumber());

                        int updatedRows = taskRepository.updateStatusAndPrStateAtomically(
                                task.getId(), "failed", task.getStatus(), prStatus.getState(), prStatus.isMerged(), LocalDateTime.now()
                        );
                        if (updatedRows > 0) {
                            reconciledCount++;
                        }
                    }
                }
            }
        }

        return reconciledCount;
    }
}
