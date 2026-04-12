package com.JobsNow.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentApplicationDTO {
    private Integer id;
    private String applicant;
    private String jobTitle;
    private String status;
    private String date;
}
