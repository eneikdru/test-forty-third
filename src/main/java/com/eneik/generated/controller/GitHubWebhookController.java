package com.eneik.generated.controller;

import com.eneik.generated.model.Task;
import com.eneik.generated.repository.TaskRepository;
import com.eneik.generated.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations/github")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final TaskRepository taskRepository;
    private final TimeProvider timeProvider;

    public GitHubWebhookController(TaskRepository taskRepository, TimeProvider timeProvider) {
        this.taskRepository = taskRepository;
        this.timeProvider = timeProvider;
    }

    @PostMapping("/webhooks")
    @Transactional
    public ResponseEntity<?> processGithubWebhook(@RequestBody Map<String, Object> payload) {
        String action = (String) payload.get("action");

        Integer prNumber = null;
        if (payload.get("number") instanceof Number num) {
            prNumber = num.intValue();
        }

        Boolean merged = null;
        String prState = null;

        if (payload.get("pull_request") instanceof Map<?, ?> prMap) {
            if (prNumber == null && prMap.get("number") instanceof Number num) {
                prNumber = num.intValue();
            }
            if (prMap.get("merged") instanceof Boolean m) {
                merged = m;
            }
            if (prMap.get("state") instanceof String s) {
                prState = s;
            }
        }

        if (merged == null && payload.get("merged") instanceof Boolean m) {
            merged = m;
        }

        if (prNumber == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST", "message", "PR number is missing"));
        }

        log.info("Received GitHub PR webhook: prNumber={}, action={}, state={}, merged={}",
                prNumber, action, prState, merged);

        List<Task> tasks = taskRepository.findByGithubPrNumber(prNumber);
        int updatedCount = 0;

        for (Task task : tasks) {
            String currentStatus = task.getStatus();

            // Check if action indicates a "closed" event
            if ("closed".equalsIgnoreCase(action) || "closed".equalsIgnoreCase(prState)) {
                boolean isMerged = (merged != null && merged);
                if (!isMerged) {
                    // PR is closed without merge: corresponding task status is updated to failed/unmerged using an atomic update.
                    // Given an existing task status that is already set to done, transition it to failed.
                    // To prevent redundant database updates, only execute update if there is a mismatch between database recorded state and polled reality.
                    if (!"failed".equalsIgnoreCase(currentStatus)
                            || !"closed".equalsIgnoreCase(task.getGithubPrState())
                            || task.getGithubPrMerged() == null
                            || task.getGithubPrMerged()) {

                        log.warn("[TELEMETRY][TASK_RECONCILIATION] GitHub PR#{} closed without merge. Transitioning task {} from {} to failed",
                                prNumber, task.getId(), currentStatus);

                        int updatedRows = taskRepository.updateStatusAndPrStateAtomically(
                                task.getId(), "failed", currentStatus, "closed", false, timeProvider.now()
                        );
                        if (updatedRows > 0) {
                            updatedCount++;
                        }
                    } else {
                        log.info("GitHub PR#{} closed without merge. Task {} is already in expected failed/unmerged state. Ignoring.",
                                prNumber, task.getId());
                    }
                } else {
                    // PR is closed and merged: transition to done if not done already
                    if (!"done".equalsIgnoreCase(currentStatus)
                            || !"closed".equalsIgnoreCase(task.getGithubPrState())
                            || task.getGithubPrMerged() == null
                            || !task.getGithubPrMerged()) {

                        log.info("GitHub PR#{} closed and merged. Transitioning task {} from {} to done",
                                prNumber, task.getId(), currentStatus);

                        int updatedRows = taskRepository.updateStatusAndPrStateAtomically(
                                task.getId(), "done", currentStatus, "closed", true, timeProvider.now()
                        );
                        if (updatedRows > 0) {
                            updatedCount++;
                        }
                    } else {
                        log.info("GitHub PR#{} closed and merged. Task {} is already in expected done state. Ignoring.",
                                prNumber, task.getId());
                    }
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "processedTasksCount", tasks.size(),
                "updatedTasksCount", updatedCount
        ));
    }
}
