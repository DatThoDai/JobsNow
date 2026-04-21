package com.JobsNow.backend.scheduler;

import com.JobsNow.backend.service.JobScoreService;
import com.JobsNow.backend.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScoreScheduler {

    private final JobScoreService jobScoreService;
    private final JobService jobService;

    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void recalculateJobScores() {
        log.info("Starting scheduled job score recalculation...");
        jobScoreService.expireBoosts();
        jobScoreService.recalculateAllScores();
        log.info("Job score recalculation completed.");
        try {
            jobService.pushJobsToAlgolia();
            log.info("Synced updated job scores to Algolia.");
        } catch (Exception e) {
            log.warn("Failed to sync updated job scores to Algolia", e);
        }
    }
}
