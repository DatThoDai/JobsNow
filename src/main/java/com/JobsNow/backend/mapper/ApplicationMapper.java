package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.ApplicationDTO;
import com.JobsNow.backend.dto.ApplicationStatusDTO;
import com.JobsNow.backend.entity.Application;
import com.JobsNow.backend.entity.ApplicationStatusHistory;
import com.JobsNow.backend.response.ApplicationDetailResponse;
import com.JobsNow.backend.response.ApplicationOfJobResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ApplicationMapper {
    public static ApplicationDTO toApplicationDTO(Application application) {
        return ApplicationDTO.builder()
                .applicationId(application.getApplicationId())
                .applicationStatus(application.getApplicationStatus())
                .appliedAt(application.getAppliedAt())
                .jobId(application.getJob().getJobId())
                .jobTitle(application.getJob().getTitle())
                .profileId(application.getJobSeekerProfile().getProfileId())
                .jobSeekerName(application.getJobSeekerProfile().getUser().getFullName())
                .jobSeekerAvatar(application.getJobSeekerProfile().getAvatarUrl())
                .resumeId(application.getResume().getResumeId())
                .resumeName(application.getResume().getResumeName())
                .resumeUrl(application.getResume().getResumeUrl())
                .build();
    }

    public static ApplicationDetailResponse toDetailResponse(Application application, List<ApplicationStatusHistory> historyList){
        return ApplicationDetailResponse.builder()
                .applicationId(application.getApplicationId())
                .job(JobMapper.toJobDTO(application.getJob()))
                .jobSeekerProfile(JobSeekerProfileMapper.toJobSeekerProfileDTO(application.getJobSeekerProfile()))
                .resumeApplied(ResumeMapper.toResumeDTO(application.getResume()))
                .statusHistory(historyList.stream()
                        .map(history -> ApplicationStatusDTO.builder()
                                .status(history.getApplicationStatus())
                                .time(history.getChangedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public static ApplicationOfJobResponse toApplicationOfJobResponse(Application application){
        return ApplicationOfJobResponse.builder()
                .applicationId(application.getApplicationId())
                .jobSeekerProfile(JobSeekerProfileMapper.toJobSeekerProfileDTO(application.getJobSeekerProfile()))
                .appliedAt(application.getAppliedAt())
                .status(application.getApplicationStatus())
                .build();
    }
}
