package com.JobsNow.backend.entity;

import com.JobsNow.backend.entity.enums.PlanType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subscription_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer planId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType type;

    private Integer durationDays;

    @Column(nullable = false)
    private Double boostScore;

    private Integer jobPostLimit;

    private Integer aiCvScanningLimit;

    private Boolean useAiCvBuilder = false;

    @Column
    private Integer priorityLevel = 0;

    @Column(nullable = false)
    private String scope = "SUBSCRIPTION";

    @Column(columnDefinition = "TEXT")
    private String description;

    private Boolean isActive = true;
}
