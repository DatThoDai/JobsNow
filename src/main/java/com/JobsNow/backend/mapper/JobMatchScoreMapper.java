package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.JobMatchScoreDTO;
import com.JobsNow.backend.entity.JobMatchScore;

public class JobMatchScoreMapper {
    public static JobMatchScoreDTO toDTO(JobMatchScore s) {
        return JobMatchScoreDTO.builder()
                .id(s.getId())
                .profileId(s.getProfile().getProfileId())
                .profileName(s.getProfile().getUser().getFullName())
                .profileTitle(s.getProfile().getTitle())
                .jobId(s.getJob().getJobId())
                .jobTitle(s.getJob().getTitle())
                .companyName(s.getJob().getCompany().getCompanyName())
                .overallScore(s.getOverallScore())
                .skillMatchScore(s.getSkillMatchScore())
                .aiSemanticScore(s.getAiSemanticScore())
                .calculatedAt(s.getCalculatedAt() != null ? s.getCalculatedAt().toString() : null)
                .build();
    }
}
