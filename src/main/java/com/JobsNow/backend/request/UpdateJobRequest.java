package com.JobsNow.backend.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateJobRequest {
    private Integer jobId;
    private String title;
    private String description;
    private String requirements;
    private String benefits;

    @PositiveOrZero
    private Double salaryMin;

    @PositiveOrZero
    private Double salaryMax;

    private String yearsOfExperience;
    private String educationLevel;
    private String jobType;
    private String location;

    @Future
    private LocalDate deadline;

    private Integer categoryId;
    private Boolean isActive;
    private String thumbnailUrl;
    private List<JobSkillItem> jobSkills;
    private List<Integer> majorIds;

    @Data
    public static class JobSkillItem {
        private Integer skillId;
        private Boolean isRequired = false;
        private String level;
    }


}
