package com.JobsNow.backend.service;

import com.JobsNow.backend.entity.SubscriptionPlan;

public interface CandidateQuotaService {
    void grantPlanBenefits(Integer userId, SubscriptionPlan plan);

    void ensureDefaultQuota(Integer userId, int initialAiMatches, int initialAiCvBuilderTrials);

    void consumeAiMatchForCandidateUser(Integer userId, int amount);

    void assertAiCvBuilderEnabledForCandidateUser(Integer userId);

    void consumeAiCvBuilderForCandidateUser(Integer userId);
}
