package com.JobsNow.backend.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationDTO {
    private Integer id;
    private String title;
    private String educationLevel;
    private Integer majorId;
    private String majorName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private Integer sortOrder;
}
