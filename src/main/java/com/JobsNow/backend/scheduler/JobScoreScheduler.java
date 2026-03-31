package com.JobsNow.backend.scheduler;

import com.JobsNow.backend.service.JobScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScoreScheduler {

    private final JobScoreService jobScoreService;

    @Scheduled(fixedRate = 600000)
    public void recalculateJobScores() {
        log.info("Starting scheduled job score recalculation...");
        jobScoreService.expireBoosts();
        jobScoreService.recalculateAllScores();
        log.info("Job score recalculation completed.");
    }
}
