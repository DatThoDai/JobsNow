package com.JobsNow.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EducationRequest {
    private Integer id;
    @NotBlank(message = "Title is required")
    private String title;
    @NotNull(message = "Education level is required")
    private String educationLevel;
    private Integer majorId;
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private Integer sortOrder;
}
