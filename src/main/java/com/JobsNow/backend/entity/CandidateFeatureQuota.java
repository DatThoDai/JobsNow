package com.JobsNow.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_feature_quota")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateFeatureQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer quotaId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer remainingAiMatches = 0;

    @Column(nullable = false)
    private Boolean isProfileHighlighted = false;

    private Integer remainingAiCvBuilderTrials = 0;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
