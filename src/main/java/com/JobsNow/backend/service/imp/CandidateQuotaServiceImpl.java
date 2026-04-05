package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.CandidateFeatureQuota;
import com.JobsNow.backend.entity.SubscriptionPlan;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.repositories.CandidateFeatureQuotaRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.service.CandidateQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CandidateQuotaServiceImpl implements CandidateQuotaService {

    private static final int DEFAULT_TRIAL_AI_CV_BUILDER_TRIALS = 3;

    private final CandidateFeatureQuotaRepository quotaRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void ensureDefaultQuota(Integer userId, int initialAiMatches, int initialAiCvBuilderTrials) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        LocalDateTime now = LocalDateTime.now();
        quotaRepository.findByUser_UserId(userId).orElseGet(() -> {
            CandidateFeatureQuota quota = CandidateFeatureQuota.builder()
                    .user(user)
                    .remainingAiMatches(Math.max(initialAiMatches, 0))
                    .isProfileHighlighted(false)
                    .remainingAiCvBuilderTrials(Math.max(initialAiCvBuilderTrials, 0))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            return quotaRepository.save(quota);
        });
    }

    @Override
    @Transactional
    public void consumeAiMatchForCandidateUser(Integer userId, int amount) {
        if (amount <= 0) {
            return;
        }

        CandidateFeatureQuota quota = quotaRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BadRequestException("No active candidate quota. Please purchase a candidate package."));

        LocalDateTime now = LocalDateTime.now();
        if (quota.getExpiresAt() != null && quota.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("Candidate subscription expired. Please renew your plan.");
        }

        int remaining = quota.getRemainingAiMatches() != null ? quota.getRemainingAiMatches() : 0;
        if (remaining < amount) {
            throw new BadRequestException("Out of AI matching quota. Please purchase or upgrade your candidate package.");
        }

        quota.setRemainingAiMatches(remaining - amount);
        quota.setUpdatedAt(now);
        quotaRepository.save(quota);
    }

    @Override
    @Transactional
    public void grantPlanBenefits(Integer userId, SubscriptionPlan plan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CandidateFeatureQuota quota = quotaRepository.findByUser_UserId(userId)
                .orElseGet(() -> CandidateFeatureQuota.builder()
                        .user(user)
                        .remainingAiMatches(0)
                        .isProfileHighlighted(false)
                .remainingAiCvBuilderTrials(DEFAULT_TRIAL_AI_CV_BUILDER_TRIALS)
                        .createdAt(LocalDateTime.now())
                        .build());

        // Accumulate remaining Ai Matches
        if (plan.getAiMatchLimit() != null && plan.getAiMatchLimit() > 0) {
            quota.setRemainingAiMatches(quota.getRemainingAiMatches() + plan.getAiMatchLimit());
        }

        if (Boolean.TRUE.equals(plan.getIsProfileHighlighted())) {
            quota.setIsProfileHighlighted(true);
        }

        if (Boolean.TRUE.equals(plan.getUseAiCvBuilder())) {
            int addBuilderTrials = plan.getAiCvScanningLimit() != null ? Math.max(plan.getAiCvScanningLimit(), 0) : 0;
            int currentTrials = quota.getRemainingAiCvBuilderTrials() != null ? quota.getRemainingAiCvBuilderTrials() : 0;
            quota.setRemainingAiCvBuilderTrials(currentTrials + addBuilderTrials);
        }

        // Extend expiration
        LocalDateTime now = LocalDateTime.now();
        if (quota.getExpiresAt() == null || quota.getExpiresAt().isBefore(now)) {
            quota.setExpiresAt(now.plusDays(plan.getDurationDays()));
        } else {
            quota.setExpiresAt(quota.getExpiresAt().plusDays(plan.getDurationDays()));
        }

        quota.setUpdatedAt(now);
        quotaRepository.save(quota);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertAiCvBuilderEnabledForCandidateUser(Integer userId) {
        CandidateFeatureQuota quota = quotaRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BadRequestException("AI CV Builder requires an active candidate plan or trial."));

        LocalDateTime now = LocalDateTime.now();
        if (quota.getExpiresAt() != null && quota.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("Candidate subscription expired. Please renew your plan.");
        }

        int remainingTrials = quota.getRemainingAiCvBuilderTrials() != null ? quota.getRemainingAiCvBuilderTrials() : 0;
        if (remainingTrials <= 0) {
            throw new BadRequestException("AI CV Builder trial exhausted. Please purchase a candidate package.");
        }
    }

    @Override
    @Transactional
    public void consumeAiCvBuilderForCandidateUser(Integer userId) {
        CandidateFeatureQuota quota = quotaRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BadRequestException("AI CV Builder requires an active candidate plan or trial."));

        LocalDateTime now = LocalDateTime.now();
        if (quota.getExpiresAt() != null && quota.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("Candidate subscription expired. Please renew your plan.");
        }

        int remainingTrials = quota.getRemainingAiCvBuilderTrials() != null ? quota.getRemainingAiCvBuilderTrials() : 0;
        if (remainingTrials <= 0) {
            throw new BadRequestException("AI CV Builder trial exhausted. Please purchase a candidate package.");
        }

        quota.setRemainingAiCvBuilderTrials(remainingTrials - 1);
        quota.setUpdatedAt(now);
        quotaRepository.save(quota);
    }
}
