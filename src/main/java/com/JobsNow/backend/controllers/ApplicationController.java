package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.ApplicationRequest;
import com.JobsNow.backend.request.SendCustomEmailRequest;
import com.JobsNow.backend.request.UpdateApplicationStatusRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PutMapping("/{applicationId}/status")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Integer applicationId,
            @RequestBody UpdateApplicationStatusRequest request) {
        applicationService.updateApplicationStatus(applicationId, request);
        return ResponseFactory.successMessage("Application status updated successfully");
    }

    @PostMapping("/{applicationId}/send-email")
    public ResponseEntity<?> sendCustomEmail(
            @PathVariable Integer applicationId,
            @Valid @RequestBody SendCustomEmailRequest request) {
        applicationService.sendCustomEmail(applicationId, request);
        return ResponseFactory.successMessage("Email sent successfully");
    }

    @PostMapping("/apply-via-email")
    public ResponseEntity<?> applyViaEmail(
            @RequestParam("email") String email,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam("jobId") Integer jobId,
            @RequestParam(value = "cvFile", required = false) MultipartFile cvFile) {
        applicationService.applyViaEmail(email, fullName, jobId, cvFile);
        return ResponseFactory.successMessage("Application submitted successfully via email");
    }

    @PostMapping("/sync-via-email")
    public ResponseEntity<?> syncViaEmail() {
        return ResponseFactory.success(applicationService.syncApplicationsFromEmail());
    }

    @PostMapping(value = "/send-apply-email", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendApplyEmail(
            @RequestParam("jobId") Integer jobId,
            @RequestParam("email") String email,
            @RequestParam("fullName") String fullName,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "body", required = false) String body,
            @RequestParam("cvFile") MultipartFile cvFile) {
        applicationService.sendApplyEmail(jobId, email, fullName, subject, body, cvFile);
        return ResponseFactory.successMessage("Application email sent successfully");
    }
}
