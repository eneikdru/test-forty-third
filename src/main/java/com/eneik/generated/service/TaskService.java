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
        List<Task> tasks = taskRepository.findAll();
        int reconciledCount = 0;

        for (Task task : tasks) {
            if (task.getGithubPrNumber() == null) {
                continue;
            }

            GitHubService.PrStatus prStatus = gitHubService.getPrStatus(task.getGithubPrNumber());

            // Check mismatch: task is marked 'done' internally, but PR closed without merge
            if ("done".equalsIgnoreCase(task.getStatus())) {
                if ("closed".equalsIgnoreCase(prStatus.getState()) && !prStatus.isMerged()) {
                    log.warn("reconcileTaskStatusAgainstGitHubTruth: task {} is marked done but PR#{} closed without merge",
                            task.getId(), task.getGithubPrNumber());

                    // Atomically guard the update status from 'done' to 'unmerged'
                    int updatedRows = taskRepository.updateStatusAtomically(task.getId(), "unmerged", "done");
                    if (updatedRows > 0) {
                        reconciledCount++;
                    }
                }
            }
        }

        return reconciledCount;
    }
}
