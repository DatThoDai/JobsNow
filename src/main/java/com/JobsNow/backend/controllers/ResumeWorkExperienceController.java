package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.WorkExperienceRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.WorkExperienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resume/{resumeId}/work-experiences")
@RequiredArgsConstructor
public class ResumeWorkExperienceController {

    private final WorkExperienceService workExperienceService;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Integer resumeId) {
        return ResponseFactory.success(workExperienceService.getByResumeId(resumeId));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Integer resumeId,
                                    @Valid @RequestBody WorkExperienceRequest request) {
        return ResponseFactory.success(workExperienceService.create(resumeId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer resumeId,
                                    @PathVariable Integer id,
                                    @Valid @RequestBody WorkExperienceRequest request) {
        return ResponseFactory.success(workExperienceService.update(resumeId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer resumeId,
                                    @PathVariable Integer id) {
        workExperienceService.delete(resumeId, id);
        return ResponseFactory.successMessage("Work experience deleted successfully");
    }
}

