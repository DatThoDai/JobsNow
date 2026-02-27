package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.ApplicationRequest;
import com.JobsNow.backend.response.BaseResponse;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/application")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;
    @PostMapping("/apply")
    public ResponseEntity<?> applyForJob(@Valid @RequestBody ApplicationRequest request) {
        applicationService.applyForJob(request);
        return ResponseFactory.successMessage("Application submitted successfully");
    }

    @GetMapping("/jobseeker/{profileId}")
    public ResponseEntity<?> getApplicationsByJobSeeker(@PathVariable Integer profileId) {
        return ResponseFactory.success(applicationService.getApplicationsByJobSeeker(profileId));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<?> getApplicationDetail(@PathVariable Integer applicationId){
        return ResponseFactory.success(applicationService.getApplicationDetail(applicationId));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getApplicationByJob(@PathVariable Integer jobId){
        return ResponseFactory.success(applicationService.getApplicationsByJob(jobId));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<?> getApplicationsByCompany(@PathVariable Integer companyId){
        return ResponseFactory.success(applicationService.getApplicationsByCompany(companyId));
    }

    @PutMapping("{applicationId}/status")
    public ResponseEntity<?> updateApplicationStatus(@PathVariable Integer applicationId, @RequestParam String status){
        applicationService.updateApplicationStatus(applicationId, status);
        return ResponseFactory.successMessage("Application status updated successfully");
    }
}
