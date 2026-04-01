package com.JobsNow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSubscriptionStatusDTO {
    private String accountStatus; // PENDING_PAYMENT, PAID_ACTIVE, EXPIRED, NO_PLAN
    private String currentPlanName;
    private Integer currentPlanId;
    private boolean active;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    
    // Quotas
    private Integer remainingAiMatches;
    private Integer remainingAiCvBuilderTrials;
    private Boolean isProfileHighlighted;

    private boolean canRepurchase;
    private boolean hasPendingOrder;
}
