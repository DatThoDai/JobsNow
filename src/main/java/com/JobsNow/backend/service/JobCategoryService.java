package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.JobCategoryDTO;
import com.JobsNow.backend.request.CreateJobCategoryRequest;
import com.JobsNow.backend.request.UpdateJobCategoryRequest;
import com.JobsNow.backend.response.PagedResponse;

import java.util.List;

public interface JobCategoryService {
    List<JobCategoryDTO> getAllJobCategories();

    PagedResponse<JobCategoryDTO> getJobCategoriesPage(int page, int limit);
    List<JobCategoryDTO> getJobCategoriesByIndustryId(Integer industryId);
    void addJobCategory(CreateJobCategoryRequest request);
    void updateJobCategory(UpdateJobCategoryRequest request);
    void deleteJobCategory(Integer categoryId);

}
