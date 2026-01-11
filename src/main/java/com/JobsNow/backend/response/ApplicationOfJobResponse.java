package com.JobsNow.backend.response;

import com.JobsNow.backend.dto.JobSeekerProfileDTO;
import com.JobsNow.backend.entity.enums.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationOfJobResponse {
    private Integer applicationId;
    private JobSeekerProfileDTO jobSeekerProfile;
    private LocalDateTime appliedAt;
    private ApplicationStatus status;
}
