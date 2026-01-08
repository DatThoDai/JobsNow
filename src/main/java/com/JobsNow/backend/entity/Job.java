package com.JobsNow.backend.entity;

import com.JobsNow.backend.entity.enums.JobType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    private Double salaryMin;

    private Double salaryMax;

    private String yearsOfExperience;

    private String educationLevel;

    private LocalDateTime postedAt;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    private String location;

    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private JobCategory category;

    @ManyToMany
    @JoinTable(
            name = "job_skill",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<Skill> skills;

    private Boolean isActive = true;

    private Boolean isDeleted = false;

    private Boolean isPending = false;

    private Boolean isApproved = false;

    private Boolean isExpired = false;
}
