package com.JobsNow.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SavedJobDTO {
    private Integer savedJobId;
    private Integer jobId;
    private String jobTitle;
    private String companyName;
    private String companyLogo;
    private String location;
    private Double salaryMin;
    private Double salaryMax;
    private String salaryType;
    private String salaryCurrency;
    private String jobType;
    private LocalDateTime savedAt;
    private Boolean isSaved;
}
