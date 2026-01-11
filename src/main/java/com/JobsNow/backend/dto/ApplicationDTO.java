package com.JobsNow.backend.dto;

import com.JobsNow.backend.entity.enums.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDTO {
    private Integer applicationId;
    private ApplicationStatus applicationStatus;
    private LocalDateTime appliedAt;

    private Integer jobId;
    private String jobTitle;

    private Integer profileId;
    private String jobSeekerName;
    private String jobSeekerAvatar;

    private Integer resumeId;
    private String resumeName;
    private String resumeUrl;
}
