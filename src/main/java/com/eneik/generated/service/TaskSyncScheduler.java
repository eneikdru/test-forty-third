package com.eneik.generated.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled component that regularly syncs internal task statuses against GitHub reality.
 */
@Component
public class TaskSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskSyncScheduler.class);

    private final TaskService taskService;

    public TaskSyncScheduler(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Executes the task status synchronization job.
     * Runs with a default fixed delay of 60 seconds, which is configurable via properties.
     */
    @Scheduled(fixedDelayString = "${task.sync.fixed-delay:60000}")
    public void runSyncJob() {
        log.info("Executing scheduled task sync job...");
        try {
            int count = taskService.syncTaskStatusesWithGitHub();
            log.info("Scheduled task sync job completed. Reconciled/updated {} tasks.", count);
        } catch (Exception e) {
            log.error("Error occurred during scheduled task synchronization", e);
        }
    }
}
