package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.EducationDTO;
import com.JobsNow.backend.entity.Education;
import org.springframework.stereotype.Component;

@Component
public class EducationMapper {
    public static EducationDTO toDTO(Education entity) {
        if (entity == null) return null;
        return EducationDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .educationLevel(entity.getEducationLevel() != null ? entity.getEducationLevel().name() : null)
                .majorId(entity.getMajor() != null ? entity.getMajor().getMajorId() : null)
                .majorName(entity.getMajor() != null ? entity.getMajor().getName() : null)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
