package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.SavedJobDTO;
import com.JobsNow.backend.entity.SavedJob;
import org.springframework.stereotype.Component;

@Component
public class SavedJobMapper {
    public SavedJobDTO toDTO(SavedJob savedJob) {
        return SavedJobDTO.builder()
                .savedJobId(savedJob.getSavedJobId())
                .jobId(savedJob.getJob().getJobId())
                .jobTitle(savedJob.getJob().getTitle())
                .companyName(savedJob.getJob().getCompany().getCompanyName())
                .companyLogo(savedJob.getJob().getCompany().getLogoUrl())
                .location(savedJob.getJob().getLocation())
                .salaryMin(savedJob.getJob().getSalaryMin())
                .salaryMax(savedJob.getJob().getSalaryMax())
                .jobType(savedJob.getJob().getJobType() != null ? savedJob.getJob().getJobType().name() : null)
                .savedAt(savedJob.getSavedAt())
                .isSaved(true)
                .build();
    }
}
