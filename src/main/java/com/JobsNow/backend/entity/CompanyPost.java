package com.JobsNow.backend.entity;

import com.JobsNow.backend.entity.enums.CompanyPostStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "company_post",
        uniqueConstraints = @UniqueConstraint(columnNames = "slug")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer postId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 500)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String featuredImageUrl;

    /** Matches FE handbook category slug */
    @Column(name = "category_key", nullable = false, length = 120)
    private String categoryKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CompanyPostStatus status;

    @Column(name = "rejection_note", columnDefinition = "TEXT")
    private String rejectionNote;

    private LocalDateTime publishedAt;
    private LocalDateTime rejectedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
