package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.CompanyFeatureQuota;
import com.JobsNow.backend.entity.SubscriptionPlan;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.repositories.CompanyFeatureQuotaRepository;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.service.CompanyQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompanyQuotaServiceImpl implements CompanyQuotaService {

    private static final int TRIAL_JOB_POSTS = 5;
    private static final int TRIAL_AI_SCANS = 5;
    private static final int TRIAL_AI_CV_BUILDER_TRIALS = 2;
    private static final int TRIAL_DURATION_DAYS = 14;

    private final CompanyFeatureQuotaRepository quotaRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public void ensureDefaultQuota(Integer companyId, int initialJobPosts) {
        quotaRepository.findByCompany_CompanyId(companyId).orElseGet(() -> {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new NotFoundException("Company not found"));
            LocalDateTime now = LocalDateTime.now();
            CompanyFeatureQuota quota = CompanyFeatureQuota.builder()
                    .company(company)
                    .remainingJobPosts(Math.max(initialJobPosts, TRIAL_JOB_POSTS))
                    .remainingAiScans(TRIAL_AI_SCANS)
                    .aiCvBuilderEnabled(true)
                    .remainingAiCvBuilderTrials(TRIAL_AI_CV_BUILDER_TRIALS)
                    .expiresAt(now.plusDays(TRIAL_DURATION_DAYS))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            return quotaRepository.save(quota);
        });
    }

    @Override
    @Transactional
    public void grantPlanBenefits(Integer companyId, SubscriptionPlan plan) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        LocalDateTime now = LocalDateTime.now();
        CompanyFeatureQuota quota = quotaRepository.findByCompany_CompanyId(companyId)
                .orElseGet(() -> CompanyFeatureQuota.builder()
                        .company(company)
                        .remainingJobPosts(0)
                        .remainingAiScans(0)
                        .aiCvBuilderEnabled(false)
                    .remainingAiCvBuilderTrials(0)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        resetIfExpired(quota, now);

        int addPosts = plan.getJobPostLimit() != null ? Math.max(plan.getJobPostLimit(), 0) : 0;
        int addScans = plan.getAiCvScanningLimit() != null ? Math.max(plan.getAiCvScanningLimit(), 0) : 0;

        quota.setRemainingJobPosts((quota.getRemainingJobPosts() != null ? quota.getRemainingJobPosts() : 0) + addPosts);
        quota.setRemainingAiScans((quota.getRemainingAiScans() != null ? quota.getRemainingAiScans() : 0) + addScans);
        quota.setAiCvBuilderEnabled(Boolean.TRUE.equals(quota.getAiCvBuilderEnabled()) || Boolean.TRUE.equals(plan.getUseAiCvBuilder()));
        if (Boolean.TRUE.equals(plan.getUseAiCvBuilder())) {
            quota.setRemainingAiCvBuilderTrials(null);
        }

        if (plan.getDurationDays() != null && plan.getDurationDays() > 0) {
            LocalDateTime base = quota.getExpiresAt() != null && quota.getExpiresAt().isAfter(now)
                    ? quota.getExpiresAt()
                    : now;
            quota.setExpiresAt(base.plusDays(plan.getDurationDays()));
        }

        quota.setUpdatedAt(now);
        quotaRepository.save(quota);
    }

    @Override
    @Transactional
    public void consumeJobPost(Integer companyId) {
        CompanyFeatureQuota quota = quotaRepository.findByCompany_CompanyId(companyId).orElse(null);
        if (quota == null) {
            ensureDefaultQuota(companyId, 5);
            quota = quotaRepository.findByCompany_CompanyId(companyId)
                    .orElseThrow(() -> new BadRequestException("No active quota. Please purchase a subscription plan."));
        }

        LocalDateTime now = LocalDateTime.now();
        if (isExpired(quota, now)) {
            resetAfterExpire(quota, now);
            quotaRepository.save(quota);
            throw new BadRequestException("Subscription expired. Please renew your plan.");
        }

        int remaining = quota.getRemainingJobPosts() != null ? quota.getRemainingJobPosts() : 0;
        if (remaining <= 0) {
            throw new BadRequestException("Out of job posting quota. Please purchase a subscription plan.");
        }

        quota.setRemainingJobPosts(remaining - 1);
        quota.setUpdatedAt(now);
        quotaRepository.save(quota);
    }

    @Override
    @Transactional
    public void consumeAiScanForCompanyUser(Integer userId, int amount) {
        if (amount <= 0) {
            return;
        }

        Company company = companyRepository.findByUser_UserId(userId).orElse(null);
        if (company == null) {
            return;
        }

        CompanyFeatureQuota quota = quotaRepository.findByCompany_CompanyId(company.getCompanyId())
                .orElseThrow(() -> new BadRequestException("No active quota. Please purchase a subscription plan."));

        LocalDateTime now = LocalDateTime.now();
        if (isExpired(quota, now)) {
            resetAfterExpire(quota, now);
            quotaRepository.save(quota);
            throw new BadRequestException("Subscription expired. Please renew your plan.");
        }

        int remaining = quota.getRemainingAiScans() != null ? quota.getRemainingAiScans() : 0;
        if (remaining < amount) {
            throw new BadRequestException("Out of AI scan quota. Please purchase a higher plan.");
        }

        quota.setRemainingAiScans(remaining - amount);
        quota.setUpdatedAt(now);
        quotaRepository.save(quota);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertAiCvBuilderEnabledForCompanyUser(Integer userId) {
        Company company = companyRepository.findByUser_UserId(userId).orElse(null);
        if (company == null) {
            return;
        }

        CompanyFeatureQuota quota = quotaRepository.findByCompany_CompanyId(company.getCompanyId())
                .orElseThrow(() -> new BadRequestException("AI CV Builder requires an active subscription."));

        LocalDateTime now = LocalDateTime.now();
        if (isExpired(quota, now) || !Boolean.TRUE.equals(quota.getAiCvBuilderEnabled())) {
            throw new BadRequestException("AI CV Builder is not enabled in your current plan.");
        }
    }

    @Override
    @Transactional
    public void consumeAiCvBuilderForCompanyUser(Integer userId) {
        Company company = companyRepository.findByUser_UserId(userId).orElse(null);
        if (company == null) {
            return;
        }

        CompanyFeatureQuota quota = quotaRepository.findByCompany_CompanyId(company.getCompanyId())
                .orElseThrow(() -> new BadRequestException("AI CV Builder requires an active subscription."));

        LocalDateTime now = LocalDateTime.now();
        if (isExpired(quota, now) || !Boolean.TRUE.equals(quota.getAiCvBuilderEnabled())) {
            throw new BadRequestException("AI CV Builder is not enabled in your current plan.");
        }

        Integer remainingTrials = quota.getRemainingAiCvBuilderTrials();
        if (remainingTrials == null) {
            return;
        }

        if (remainingTrials <= 0) {
            throw new BadRequestException("AI CV Builder trial exhausted. Please purchase a plan.");
        }

        quota.setRemainingAiCvBuilderTrials(remainingTrials - 1);
        quota.setUpdatedAt(now);
        quotaRepository.save(quota);
    }

    private void resetIfExpired(CompanyFeatureQuota quota, LocalDateTime now) {
        if (isExpired(quota, now)) {
            resetAfterExpire(quota, now);
        }
    }

    private boolean isExpired(CompanyFeatureQuota quota, LocalDateTime now) {
        return quota.getExpiresAt() != null && quota.getExpiresAt().isBefore(now);
    }

    private void resetAfterExpire(CompanyFeatureQuota quota, LocalDateTime now) {
        quota.setRemainingJobPosts(0);
        quota.setRemainingAiScans(0);
        quota.setAiCvBuilderEnabled(false);
        quota.setRemainingAiCvBuilderTrials(0);
        quota.setExpiresAt(null);
        quota.setUpdatedAt(now);
    }
}
