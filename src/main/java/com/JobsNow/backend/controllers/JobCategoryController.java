package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.CreateJobCategoryRequest;
import com.JobsNow.backend.request.UpdateJobCategoryRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.JobCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class JobCategoryController {
    private final JobCategoryService jobCategoryService;
    @GetMapping("/all")
    public ResponseEntity<?> getAllJobCategories() {
        return ResponseFactory.success(jobCategoryService.getAllJobCategories());
    }

    @GetMapping("/industry/{industryId}")
    public ResponseEntity<?> getJobCategoriesByIndustryId(@PathVariable Integer industryId){
        return ResponseFactory.success(jobCategoryService.getJobCategoriesByIndustryId(industryId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addJobCategory(@RequestBody CreateJobCategoryRequest request){
        jobCategoryService.addJobCategory(request);
        return ResponseFactory.successMessage("Job category added successfully");
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateJobCategory(@RequestBody UpdateJobCategoryRequest request){
        jobCategoryService.updateJobCategory(request);
        return ResponseFactory.successMessage("Job category updated successfully");
    }

    @DeleteMapping("/delete/{categoryId}")
    public ResponseEntity<?> deleteJobCategory(@PathVariable Integer categoryId){
        jobCategoryService.deleteJobCategory(categoryId);
        return ResponseFactory.successMessage("Job category deleted successfully");
    }

}
