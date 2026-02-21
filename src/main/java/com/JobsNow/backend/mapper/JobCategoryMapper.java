package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.JobCategoryDTO;
import com.JobsNow.backend.entity.JobCategory;
import org.springframework.stereotype.Component;

@Component
public class JobCategoryMapper {
    public static JobCategoryDTO toJobCategoryDTO(JobCategory jobCategory) {
        return JobCategoryDTO.builder()
                .categoryId(jobCategory.getId())
                .categoryName(jobCategory.getName())
                .industryId(jobCategory.getIndustry().getIndustryId())
                .industryName(jobCategory.getIndustry().getName())
                .build();
    }
}
