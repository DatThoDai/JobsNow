package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.WorkExperienceDTO;
import com.JobsNow.backend.entity.WorkExperience;
import com.JobsNow.backend.entity.enums.WorkExperienceLevel;
import org.springframework.stereotype.Component;

@Component
public class WorkExperienceMapper {
    public static WorkExperienceDTO toDTO(WorkExperience entity) {
        if (entity == null) return null;
        return WorkExperienceDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .level(entity.getLevel() != null ? entity.getLevel().name() : null)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
