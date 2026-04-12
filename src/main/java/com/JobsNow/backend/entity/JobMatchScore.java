package com.JobsNow.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_match_score",
       uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "job_id"}))
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class JobMatchScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private JobSeekerProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    private Integer overallScore;
    private Integer skillMatchScore;
    private Integer aiSemanticScore;
    private LocalDateTime calculatedAt;
}
