package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.CreateResumeRequest;
import com.JobsNow.backend.response.BaseResponse;
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
        BaseResponse response = new BaseResponse();
        resumeService.createResume(profileId, request);
        response.setCode(200);
        response.setMessage("Resume created successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{resumeId}")
    public ResponseEntity<?> deleteResume(@PathVariable Integer resumeId) {
        BaseResponse response = new BaseResponse();
        resumeService.deleteResume(resumeId);
        response.setCode(200);
        response.setMessage("Resume deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile/{profileId}")
    public ResponseEntity<?> getResumeById(@PathVariable Integer profileId) {
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Resume retrieved successfully");
        response.setData(resumeService.getResumesByProfileId(profileId));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{resumeId}/set-primary")
    public ResponseEntity<?> setPrimaryResume(@PathVariable Integer resumeId, @RequestParam Integer profileId) {
        BaseResponse response = new BaseResponse();
        resumeService.setPrimaryResume(resumeId, profileId);
        response.setCode(200);
        response.setMessage("Primary resume set successfully");
        return ResponseEntity.ok(response);
    }
}
