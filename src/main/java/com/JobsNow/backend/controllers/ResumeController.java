package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.CreateResumeRequest;
import com.JobsNow.backend.response.BaseResponse;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resume")
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeService resumeService;
    @PostMapping("/create/{profileId}")
    public ResponseEntity<?> createResume(@PathVariable Integer profileId, @ModelAttribute CreateResumeRequest request) {
        resumeService.createResume(profileId, request);
        return ResponseFactory.successMessage("Resume created successfully");
    }

    @DeleteMapping("/delete/{resumeId}")
    public ResponseEntity<?> deleteResume(@PathVariable Integer resumeId) {
        resumeService.deleteResume(resumeId);
        return ResponseFactory.successMessage("Resume deleted successfully");
    }

    @GetMapping("/profile/{profileId}")
    public ResponseEntity<?> getResumeById(@PathVariable Integer profileId) {
        return ResponseFactory.success(resumeService.getResumesByProfileId(profileId));
    }

    @PutMapping("/{resumeId}/set-primary")
    public ResponseEntity<?> setPrimaryResume(@PathVariable Integer resumeId, @RequestParam Integer profileId) {
        resumeService.setPrimaryResume(resumeId, profileId);
        return ResponseFactory.successMessage("Primary resume set successfully");
    }
}
