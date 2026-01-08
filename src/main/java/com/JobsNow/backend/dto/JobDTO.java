package com.JobsNow.backend.dto;
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
    private String yearsOfExperience;
    private String educationLevel;
    private String jobType;
    private String location;
    private LocalDateTime postedAt;
    private LocalDate deadline;
    private Boolean isActive;

    private Integer companyId;
    private String companyName;
    private String companyLogo;

    private Integer categoryId;
    private String categoryName;

    private List<SkillDTO> skills;
}
