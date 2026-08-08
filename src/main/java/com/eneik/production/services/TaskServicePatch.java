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
            if ("closed".equalsIgnoreCase(prStatus.getState()) && !prStatus.isMerged()) {
                throw new IllegalStateException(
                        "Cannot transition task status to 'done' because associated PR #"
                        + task.getGithubPrNumber() + " is closed without being merged."
                );
            }
        }

        int updated = taskRepository.updateStatusAtomically(id, targetStatus, task.getStatus());
        if (updated == 0) {
            throw new IllegalStateException("Task status was modified concurrently or concurrent state change");
        }
        return taskRepository.findById(id).orElseThrow();
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

            if ("done".equalsIgnoreCase(task.getStatus())) {
                if ("closed".equalsIgnoreCase(prStatus.getState()) && !prStatus.isMerged()) {
                    log.warn("[TELEMETRY][TASK_RECONCILIATION] syncTaskStatusesWithGitHub: task {} is marked done but PR#{} closed without merge",
                            task.getId(), task.getGithubPrNumber());

                    int updatedRows = taskRepository.updateStatusAndPrStateAtomically(
                            task.getId(), "failed", task.getStatus(), prStatus.getState(), prStatus.isMerged(), timeProvider.now()
                    );
                    if (updatedRows > 0) {
                        reconciledCount++;
                    }
                }
            } else {
                if ("closed".equalsIgnoreCase(prStatus.getState())) {
                    if (prStatus.isMerged()) {
                        log.info("syncTaskStatusesWithGitHub: task {} has merged PR#{}, transitioning status to done",
                                task.getId(), task.getGithubPrNumber());

                        int updatedRows = taskRepository.updateStatusAndPrStateAtomically(
                                task.getId(), "done", task.getStatus(), prStatus.getState(), prStatus.isMerged(), timeProvider.now()
                        );
                        if (updatedRows > 0) {
                            reconciledCount++;
                        }
                    } else {
                        log.info("syncTaskStatusesWithGitHub: task {} has unmerged closed PR#{}, retaining status {}",
                                task.getId(), task.getGithubPrNumber(), task.getStatus());

                        int updatedRows = taskRepository.updateStatusAndPrStateAtomically(
                                task.getId(), task.getStatus(), task.getStatus(), prStatus.getState(), prStatus.isMerged(), timeProvider.now()
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
