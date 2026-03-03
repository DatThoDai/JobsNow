package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.JobDTO;
import com.JobsNow.backend.dto.JobSkillDTO;
import com.JobsNow.backend.entity.Job;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        dto.setEducationLevel(job.getEducationLevel() != null ? job.getEducationLevel().name() : null);
        dto.setJobType(job.getJobType() != null ? job.getJobType().name() : null);
        dto.setLocation(job.getLocation());
        dto.setPostedAt(job.getPostedAt());
        dto.setDeadline(job.getDeadline());
        dto.setIsActive(job.getIsActive());
        dto.setThumbnailUrl(job.getThumbnailUrl());

        if (job.getCompany() != null) {
            dto.setCompanyId(job.getCompany().getCompanyId());
            dto.setCompanyName(job.getCompany().getCompanyName());
            dto.setCompanyLogo(job.getCompany().getLogoUrl());
        }

        if (job.getCategory() != null) {
            dto.setCategoryId(job.getCategory().getId());
            dto.setCategoryName(job.getCategory().getName());
        }

        if (job.getJobSkills() != null && !job.getJobSkills().isEmpty()) {
            dto.setJobSkills(job.getJobSkills().stream()
                    .map(js -> JobSkillDTO.builder()
                            .skillId(js.getSkill().getSkillId())
                            .skillName(js.getSkill().getSkillName())
                            .isRequired(js.getIsRequired())
                            .level(js.getLevel())
                            .build())
                    .collect(Collectors.toList()));
        } else {
            dto.setJobSkills(new ArrayList<>());
        }

        dto.setMajors( job.getMajors() != null ? job.getMajors() : new ArrayList<>());

        return dto;
    }
}
