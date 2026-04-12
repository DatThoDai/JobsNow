package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.ProjectDTO;
import com.JobsNow.backend.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {
    public static ProjectDTO toDTO(Project entity) {
        if (entity == null) return null;
        return ProjectDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
