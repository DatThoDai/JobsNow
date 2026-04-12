package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.ProjectRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resume/{resumeId}/projects")
@RequiredArgsConstructor
public class ResumeProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Integer resumeId) {
        return ResponseFactory.success(projectService.getByResumeId(resumeId));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Integer resumeId,
                                    @Valid @RequestBody ProjectRequest request) {
        return ResponseFactory.success(projectService.create(resumeId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer resumeId,
                                    @PathVariable Integer id,
                                    @Valid @RequestBody ProjectRequest request) {
        return ResponseFactory.success(projectService.update(resumeId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer resumeId,
                                    @PathVariable Integer id) {
        projectService.delete(resumeId, id);
        return ResponseFactory.successMessage("Project deleted successfully");
    }
}

