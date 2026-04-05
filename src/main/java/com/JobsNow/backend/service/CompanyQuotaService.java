package com.JobsNow.backend.service;

import com.JobsNow.backend.entity.SubscriptionPlan;

public interface CompanyQuotaService {
    void ensureDefaultQuota(Integer companyId, int initialJobPosts);

    void grantPlanBenefits(Integer companyId, SubscriptionPlan plan);

    void consumeJobPost(Integer companyId);

    void consumeAiScanForCompanyUser(Integer userId, int amount);

    void assertAiCvBuilderEnabledForCompanyUser(Integer userId);

    void consumeAiCvBuilderForCompanyUser(Integer userId);
}
