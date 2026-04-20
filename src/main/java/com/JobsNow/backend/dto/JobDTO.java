package com.JobsNow.backend.dto;
import com.JobsNow.backend.entity.Major;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDTO {
    private Integer jobId;
    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private Double salaryMin;
    private Double salaryMax;
    private String salaryType;
    private String salaryCurrency;
    private String yearsOfExperience;
    private String educationLevel;
    private String jobType;
    private String location;
    private LocalDateTime postedAt;
    private LocalDate deadline;
    private Boolean isActive;
    private Boolean isApproved;
    private Boolean isPending;
    private Boolean isDeleted;
    private Boolean isExpired;
    private String thumbnailUrl;
    private String note;

    private Integer companyId;
    private String companyName;
    private String companyLogo;

    private Integer categoryId;
    private String categoryName;

    private String applicationLanguage;
    private String genderRequirement;
    private Integer minAge;
    private Integer maxAge;

    /** Denormalized from company for job detail */
    private String contactPersonName;
    private String contactTutorial;
    private String companyAddress;
    private List<SocialDTO> companySocials;

    private List<JobSkillDTO> jobSkills;
    private List<Major> majors;

    private Integer viewCount;
    private Integer applyCount;
    private Double baseScore;
    private Double boostScore;
    private Double finalScore;
    private String hotTag;
    private Boolean boostActive;
    private String activeBoostPlanType;
    private LocalDateTime activeBoostEndAt;
}
