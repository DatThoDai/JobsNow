package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.JobDTO;
import com.JobsNow.backend.entity.Job;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JobMapper {
    public static JobDTO toJobDTO(Job job) {
        JobDTO dto = new JobDTO();
        dto.setJobId(job.getJobId());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setRequirements(job.getRequirements());
        dto.setBenefits(job.getBenefits());
        dto.setSalaryMin(job.getSalaryMin());
        dto.setSalaryMax(job.getSalaryMax());
        dto.setYearsOfExperience(job.getYearsOfExperience());
        dto.setEducationLevel(job.getEducationLevel());
        dto.setJobType(job.getJobType() != null ? job.getJobType().name() : null);
        dto.setLocation(job.getLocation());
        dto.setPostedAt(job.getPostedAt());
        dto.setDeadline(job.getDeadline());
        dto.setIsActive(job.getIsActive());

        if (job.getCompany() != null) {
            dto.setCompanyId(job.getCompany().getCompanyId());
            dto.setCompanyName(job.getCompany().getCompanyName());
            dto.setCompanyLogo(job.getCompany().getLogoUrl());
        }

        if (job.getCategory() != null) {
            dto.setCategoryId(job.getCategory().getId());
            dto.setCategoryName(job.getCategory().getName());
        }

        if (job.getSkills() != null && !job.getSkills().isEmpty()) {
            dto.setSkills(job.getSkills().stream()
                    .map(SkillMapper::toSkillDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
