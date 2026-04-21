package com.JobsNow.backend.entity;

import com.JobsNow.backend.entity.enums.ApplicationLanguage;
import com.JobsNow.backend.entity.enums.EducationLevel;
import com.JobsNow.backend.entity.enums.GenderRequirement;
import com.JobsNow.backend.entity.enums.JobType;
import com.JobsNow.backend.entity.enums.SalaryCurrency;
import com.JobsNow.backend.entity.enums.SalaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.JobsNow.backend.entity.enums.JobHotTag;
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

    @Enumerated(EnumType.STRING)
    private SalaryType salaryType = SalaryType.RANGE;

    @Enumerated(EnumType.STRING)
    private SalaryCurrency salaryCurrency = SalaryCurrency.VND;

    private String yearsOfExperience;

    @Enumerated(EnumType.STRING)
    private EducationLevel educationLevel;

    private LocalDateTime postedAt;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    private ApplicationLanguage applicationLanguage;

    @Enumerated(EnumType.STRING)
    private GenderRequirement genderRequirement;

    private Integer minAge;

    private Integer maxAge;

    private String location;

    private String note;

    @Column(columnDefinition = "TEXT")
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private JobCategory category;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobSkill> jobSkills;

    @ManyToMany
    @JoinTable(
            name = "job_major",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "major_id")
    )
    private List<Major> majors;

    private Boolean isActive = true;

    private Boolean isDeleted = false;

    private Boolean isPending = false;

    private Boolean isApproved = false;

    private Boolean isExpired = false;

    private Integer viewCount = 0;

    private Integer applyCount = 0;

    private Double baseScore = 0.0;

    private Double boostScore = 0.0;

    private Double finalScore = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private JobHotTag hotTag = JobHotTag.NORMAL;
}
