package com.JobsNow.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_feature_quota")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyFeatureQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer quotaId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(nullable = false)
    private Integer remainingJobPosts = 0;

    @Column(nullable = false)
    private Integer remainingAiScans = 0;

    @Column(nullable = false)
    private Boolean aiCvBuilderEnabled = false;

    private Integer remainingAiCvBuilderTrials = 0;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
