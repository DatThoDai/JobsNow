package com.JobsNow.backend.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkExperienceDTO {
    private Integer id;
    private String title;
    private String level;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private Integer sortOrder;
}
