package com.JobsNow.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CompanySubscriptionStatusDTO {
    private String accountStatus;
    private Integer currentPlanId;
    private String currentPlanName;
    private String currentPlanType;
    private Boolean active;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private Integer remainingJobPosts;
    private Integer remainingAiScans;
    private Boolean aiCvBuilderEnabled;
    private Integer remainingAiCvBuilderTrials;
    private Boolean canRepurchase;
    private Boolean hasPendingOrder;
}
