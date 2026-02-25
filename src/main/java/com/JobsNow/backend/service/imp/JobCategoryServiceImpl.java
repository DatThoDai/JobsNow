package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.JobCategoryDTO;
import com.JobsNow.backend.entity.Industry;
import com.JobsNow.backend.entity.JobCategory;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.JobCategoryMapper;
import com.JobsNow.backend.repositories.IndustryRepository;
import com.JobsNow.backend.repositories.JobCategoryRepository;
import com.JobsNow.backend.request.CreateJobCategoryRequest;
import com.JobsNow.backend.request.UpdateJobCategoryRequest;
import com.JobsNow.backend.service.JobCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobCategoryServiceImpl implements JobCategoryService {
    private final JobCategoryRepository jobCategoryRepository;
    private final IndustryRepository industryRepository;
    @Override
    public List<JobCategoryDTO> getAllJobCategories() {
        List<JobCategory> jobCategories = jobCategoryRepository.findAll();
        return jobCategories.stream()
                .map(JobCategoryMapper::toJobCategoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobCategoryDTO> getJobCategoriesByIndustryId(Integer industryId) {
        List<JobCategory> jobCategories = jobCategoryRepository.findByIndustry_IndustryId(industryId);
        return jobCategories.stream()
                .map(JobCategoryMapper::toJobCategoryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void addJobCategory(CreateJobCategoryRequest request) {
        if(jobCategoryRepository.existsByName(request.getCategoryName())) {
            throw new BadRequestException("Job category already exists.");
        }
        Industry industry = industryRepository.findById(request.getIndustryId())
                .orElseThrow(() -> new NotFoundException("Industry not found"));
        JobCategory jobCategory = JobCategory.builder()
                .name(request.getCategoryName().trim())
                .industry(industry)
                .build();
        jobCategoryRepository.save(jobCategory);
    }

    @Override
    public void updateJobCategory(UpdateJobCategoryRequest request) {
        JobCategory category = jobCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Job category not found."));
        if(jobCategoryRepository.existsByName(request.getCategoryName().trim())){
            throw new BadRequestException("Job category name already exists.");
        }
        category.setName(request.getCategoryName().trim());
        if (request.getIndustryId() != null) {
            Industry industry = industryRepository.findById(request.getIndustryId())
                    .orElseThrow(() -> new NotFoundException("Industry not found"));
            category.setIndustry(industry);
        } else {
            category.setIndustry(null);
        }
        jobCategoryRepository.save(category);
    }

    @Override
    public void deleteJobCategory(Integer categoryId) {
        if(!jobCategoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Job category not found.");
        }
        jobCategoryRepository.deleteById(categoryId);
    }
}
