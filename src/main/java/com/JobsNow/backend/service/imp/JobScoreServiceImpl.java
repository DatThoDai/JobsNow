package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Job;
import com.JobsNow.backend.entity.JobBoost;
import com.JobsNow.backend.entity.enums.JobHotTag;
import com.JobsNow.backend.entity.enums.PlanType;
import com.JobsNow.backend.repositories.JobBoostRepository;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.service.JobScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobScoreServiceImpl implements JobScoreService {

    private static final double MAX_BOOST_CAP = 0.6;
    private static final double MAX_MULTIPLIER = 2.0;

    private final JobRepository jobRepository;
    private final JobBoostRepository jobBoostRepository;

    @Override
    @Transactional
    public void recalculateAllScores() {
        List<Job> activeJobs = jobRepository.findByIsActiveTrueAndIsDeletedFalse();

        if (activeJobs.isEmpty()) return;

        int maxViews = activeJobs.stream()
                .mapToInt(j -> j.getViewCount() != null ? j.getViewCount() : 0)
                .max().orElse(1);
        int maxApplies = activeJobs.stream()
                .mapToInt(j -> j.getApplyCount() != null ? j.getApplyCount() : 0)
                .max().orElse(1);

        double maxCtr = activeJobs.stream()
                .mapToDouble(j -> {
                    int views = j.getViewCount() != null ? j.getViewCount() : 0;
                    int applies = j.getApplyCount() != null ? j.getApplyCount() : 0;
                    return views > 0 ? (double) applies / views : 0;
                })
                .max().orElse(1);

        if (maxViews == 0) maxViews = 1;
        if (maxApplies == 0) maxApplies = 1;
        if (maxCtr == 0) maxCtr = 1;

        for (Job job : activeJobs) {
            int views = job.getViewCount() != null ? job.getViewCount() : 0;
            int applies = job.getApplyCount() != null ? job.getApplyCount() : 0;

            double viewsNorm = (double) views / maxViews;
            double applyNorm = (double) applies / maxApplies;
            double ctr = views > 0 ? (double) applies / views : 0;
            double ctrNorm = ctr / maxCtr;

            long daysSincePost = 0;
            if (job.getPostedAt() != null) {
                daysSincePost = Duration.between(job.getPostedAt(), LocalDateTime.now()).toDays();
            }
            double freshness = Math.exp(-daysSincePost / 7.0);

            double baseScore = 0.25 * viewsNorm + 0.30 * applyNorm + 0.20 * ctrNorm + 0.25 * freshness;

            double boost = job.getBoostScore() != null ? job.getBoostScore() : 0.0;
            double effectiveBoost = Math.min(boost, MAX_BOOST_CAP);
            double finalScore = Math.min(1.0, baseScore * (1.0 + Math.min(effectiveBoost, MAX_MULTIPLIER)));

            PlanType activePlanType = jobBoostRepository.findByJob_JobIdAndIsActiveTrue(job.getJobId())
                    .map(b -> b.getPlan() != null ? b.getPlan().getType() : null)
                    .orElse(null);

            JobHotTag tag = JobHotTag.NORMAL;
            if (activePlanType == PlanType.VIP) {
                tag = JobHotTag.SUPER_HOT;
            } else if (boost >= 0.8) {
                tag = JobHotTag.SUPER_HOT;
            } else if (boost >= 0.3) {
                tag = JobHotTag.HOT;
            } else if (baseScore >= 0.7) {
                tag = JobHotTag.SUPER_HOT;
            } else if (baseScore >= 0.5 || boost > 0) {
                tag = JobHotTag.HOT;
            }

            job.setBaseScore(baseScore);
            job.setFinalScore(finalScore);
            job.setHotTag(tag);
        }

        jobRepository.saveAll(activeJobs);
        log.info("Recalculated scores for {} jobs", activeJobs.size());
    }

    @Override
    @Transactional
    public void expireBoosts() {
        List<JobBoost> expiredBoosts = jobBoostRepository.findByIsActiveTrueAndEndAtBefore(LocalDateTime.now());

        for (JobBoost boost : expiredBoosts) {
            boost.setIsActive(false);
            jobBoostRepository.save(boost);

            Job job = boost.getJob();
            job.setBoostScore(0.0);
            jobRepository.save(job);

            log.info("Expired boost for job: {}", job.getJobId());
        }
    }
}
