package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.ApplicationRequest;
import com.JobsNow.backend.response.BaseResponse;
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
        BaseResponse response = new BaseResponse();
        applicationService.applyForJob(request);
        response.setCode(200);
        response.setMessage("Applied for job successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobseeker/{profileId}")
    public ResponseEntity<?> getApplicationsByJobSeeker(@PathVariable Integer profileId) {
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Applications retrieved successfully");
        response.setData(applicationService.getApplicationsByJobSeeker(profileId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<?> getApplicationDetail(@PathVariable Integer applicationId){
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Application detail retrieved successfully");
        response.setData(applicationService.getApplicationDetail(applicationId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getApplicationByJob(@PathVariable Integer jobId){
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Job applications retrieved successfully");
        response.setData(applicationService.getApplicationsByJob(jobId));
        return ResponseEntity.ok(response);
    }

    @PutMapping("{applicationId}/status")
    public ResponseEntity<?> updateApplicationStatus(@PathVariable Integer applicationId, @RequestParam String status){
        BaseResponse response = new BaseResponse();
        applicationService.updateApplicationStatus(applicationId, status);
        response.setCode(200);
        response.setMessage("Application status updated successfully");
        return ResponseEntity.ok(response);
    }
}
