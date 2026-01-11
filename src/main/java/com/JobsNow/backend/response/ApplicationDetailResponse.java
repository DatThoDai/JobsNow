package com.JobsNow.backend.response;

import com.JobsNow.backend.dto.ApplicationStatusDTO;
import com.JobsNow.backend.dto.JobDTO;
import com.JobsNow.backend.dto.JobSeekerProfileDTO;
import com.JobsNow.backend.dto.ResumeDTO;
import com.JobsNow.backend.entity.Resume;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDetailResponse {
    private Integer applicationId;
    private JobDTO job;
    private JobSeekerProfileDTO jobSeekerProfile;
    private ResumeDTO resumeApplied;
    private List<ApplicationStatusDTO> statusHistory;
}
