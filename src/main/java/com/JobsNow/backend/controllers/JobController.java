package com.JobsNow.backend.controllers;

import com.JobsNow.backend.dto.JobDTO;
import com.JobsNow.backend.request.CreateJobRequest;
import com.JobsNow.backend.request.RejectJobRequest;
import com.JobsNow.backend.request.UpdateJobRequest;
import com.JobsNow.backend.response.BaseResponse;
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
        BaseResponse response = new BaseResponse();
        List<JobDTO> jobs = jobService.getAllJobs();
        response.setCode(200);
        response.setMessage("Jobs retrieved successfully");
        response.setData(jobs);
        return ResponseEntity.ok(response);
    }

    @GetMapping ("/{jobId}")
    public ResponseEntity<?> getJobById(@PathVariable Integer jobId) {
        BaseResponse response = new BaseResponse();
        JobDTO job = jobService.getJobById(jobId);
        response.setCode(200);
        response.setMessage("Job details retrieved successfully");
        response.setData(job);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/searchJobs")
    public ResponseEntity<?> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> location,
            @RequestParam(required = false) Integer categoryId) {
        BaseResponse response = new BaseResponse();
        List<JobDTO> jobs = jobService.searchJobs(keyword, location, categoryId);
        response.setCode(200);
        response.setMessage("Jobs search completed");
        response.setData(jobs);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<?> getJobsByCompanyId(@PathVariable Integer companyId) {
        BaseResponse response = new BaseResponse();
        List<JobDTO> jobs = jobService.getJobsByCompanyId(companyId);
        response.setCode(200);
        response.setMessage("Company jobs retrieved successfully");
        response.setData(jobs);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createJob(@Valid @RequestBody CreateJobRequest request) {
        BaseResponse response = new BaseResponse();
        jobService.createJob(request);
        response.setCode(200);
        response.setMessage("Job created successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<?> updateJob(@PathVariable Integer jobId, @Valid @RequestBody UpdateJobRequest request) {
        BaseResponse response = new BaseResponse();
        request.setJobId(jobId);
        jobService.updateJob(request);
        response.setCode(200);
        response.setMessage("Job updated successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> deleteJob(@PathVariable Integer jobId) {
        BaseResponse response = new BaseResponse();
        jobService.deleteJob(jobId);
        response.setCode(200);
        response.setMessage("Job deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/approve/{jobId}")
    public ResponseEntity<?> approveJob(@PathVariable Integer jobId) {
        BaseResponse response = new BaseResponse();
        jobService.approveJob(jobId);
        response.setCode(200);
        response.setMessage("Job approved successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/reject")
    public ResponseEntity<?> rejectJob(@RequestBody RejectJobRequest request) {
        BaseResponse response = new BaseResponse();
        jobService.rejectJob(request);
        response.setCode(200);
        response.setMessage("Job rejected successfully");
        return ResponseEntity.ok(response);
    }


}
