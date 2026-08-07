package com.eneik.generated.service;

import com.eneik.generated.model.Task;
import com.eneik.generated.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
     * updates the state to 'unmerged' using an atomically-guarded query.
     */
    public int reconcileTaskStatusAgainstGitHubTruth() {
        return syncTaskStatusesWithGitHub();
    }

    /**
     * Syncs tasks status with GitHub reality.
     * If a task is marked 'done' but its associated GitHub PR is closed and unmerged, transitions status to 'unmerged'.
     * If a task is not marked 'done' but its associated GitHub PR is closed and merged, transitions status to 'done'.
     * If a task is not marked 'done' and its associated GitHub PR is closed and unmerged, bypasses status update to 'done'.
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
                // Task is done but PR is closed and unmerged -> update status to unmerged
                if ("closed".equalsIgnoreCase(prStatus.getState()) && !prStatus.isMerged()) {
                    log.warn("syncTaskStatusesWithGitHub: task {} is marked done but PR#{} closed without merge",
                            task.getId(), task.getGithubPrNumber());

                    int updatedRows = taskRepository.updateStatusAtomically(task.getId(), "unmerged", "done");
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

                        int updatedRows = taskRepository.updateStatusAtomically(task.getId(), "done", task.getStatus());
                        if (updatedRows > 0) {
                            reconciledCount++;
                        }
                    } else {
                        // PR is closed and unmerged -> status update logic to done is bypassed
                        log.info("syncTaskStatusesWithGitHub: task {} has unmerged closed PR#{}, bypassing status update to done",
                                task.getId(), task.getGithubPrNumber());
                    }
                }
            }
        }

        return reconciledCount;
    }
}
