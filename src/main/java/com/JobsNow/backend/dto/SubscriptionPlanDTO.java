package com.JobsNow.backend.dto;

import com.JobsNow.backend.entity.enums.PlanType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanDTO {
    private Integer planId;
    private String name;
    private Double price;
    private PlanType type;
    private Integer durationDays;
    private Double boostScore;
    private Integer jobPostLimit;
    private Integer aiCvScanningLimit;
    private Boolean useAiCvBuilder;
    private Integer priorityLevel;
    private String scope;
    private String targetAudience;
    private Integer aiMatchLimit;
    private Boolean isProfileHighlighted;
    private String description;
}
