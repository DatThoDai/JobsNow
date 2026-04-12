package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.EducationRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.EducationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resume/{resumeId}/educations")
@RequiredArgsConstructor
public class ResumeEducationController {

    private final EducationService educationService;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Integer resumeId) {
        return ResponseFactory.success(educationService.getByResumeId(resumeId));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Integer resumeId,
                                    @Valid @RequestBody EducationRequest request) {
        return ResponseFactory.success(educationService.create(resumeId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer resumeId,
                                    @PathVariable Integer id,
                                    @Valid @RequestBody EducationRequest request) {
        return ResponseFactory.success(educationService.update(resumeId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer resumeId,
                                    @PathVariable Integer id) {
        educationService.delete(resumeId, id);
        return ResponseFactory.successMessage("Education deleted successfully");
    }
}

