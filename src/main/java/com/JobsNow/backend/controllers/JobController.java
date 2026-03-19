package com.JobsNow.backend.controllers;

import com.JobsNow.backend.dto.JobDTO;
import com.JobsNow.backend.request.CreateJobRequest;
import com.JobsNow.backend.request.RejectJobRequest;
import com.JobsNow.backend.request.UpdateJobRequest;
import com.JobsNow.backend.response.BaseResponse;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    @GetMapping
    public ResponseEntity<?> getAllJobs() {
        return ResponseFactory.success( jobService.getAllJobs());
    }

    @GetMapping ("/{jobId}")
    public ResponseEntity<?> getJobById(@PathVariable Integer jobId) {
        return ResponseFactory.success( jobService.getJobById(jobId));
    }

    @GetMapping("/searchJobs")
    public ResponseEntity<?> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> location,
            @RequestParam(required = false) List<Integer> categoryIds) {
        return ResponseFactory.success(jobService.searchJobs(keyword, location, categoryIds));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<?> getJobsByCompanyId(@PathVariable Integer companyId) {
        return ResponseFactory.success( jobService.getJobsByCompanyId(companyId));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createJob(@Valid @RequestBody CreateJobRequest request) {
        jobService.createJob(request);
        return ResponseFactory.successMessage("Job created successfully");
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<?> updateJob(@PathVariable Integer jobId, @Valid @RequestBody UpdateJobRequest request) {
        request.setJobId(jobId);
        jobService.updateJob(request);
        return ResponseFactory.successMessage("Job updated successfully");
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> deleteJob(@PathVariable Integer jobId) {
        jobService.deleteJob(jobId);
        return ResponseFactory.successMessage("Job deleted successfully");
    }

    @PutMapping("/approve/{jobId}")
    public ResponseEntity<?> approveJob(@PathVariable Integer jobId) {
        jobService.approveJob(jobId);
        return ResponseFactory.successMessage("Job approved successfully");
    }

    @PutMapping("/reject")
    public ResponseEntity<?> rejectJob(@RequestBody RejectJobRequest request) {
        jobService.rejectJob(request);
        return ResponseFactory.successMessage("Job rejected successfully");
    }


}
