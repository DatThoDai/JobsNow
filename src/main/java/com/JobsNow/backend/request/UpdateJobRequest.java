package com.JobsNow.backend.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateJobRequest {
    private Integer jobId;

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Job description is required")
    private String description;

    @NotBlank(message = "Job requirements is required")
    private String requirements;

    @NotBlank(message = "Job benefits is required")
    private String benefits;

    @PositiveOrZero(message = "Minimum salary must be >= 0")
    private Double salaryMin;

    @PositiveOrZero(message = "Maximum salary must be >= 0")
    private Double salaryMax;

    @NotBlank(message = "Experience is required")
    private String yearsOfExperience;

    @NotBlank(message = "Education level is required")
    private String educationLevel;

    private String jobType;

    @NotBlank(message = "Location is required")
    private String location;

    @Future(message = "Deadline must be a future date")
    private LocalDate deadline;

    private Integer categoryId;

    private List<Integer> skillIds;

    private Boolean isActive;
}
