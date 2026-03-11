package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.ResumeSkillRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.ResumeSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resume/{resumeId}/skills")
@RequiredArgsConstructor
public class ResumeSkillController {

    private final ResumeSkillService resumeSkillService;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Integer resumeId) {
        return ResponseFactory.success(resumeSkillService.getByResumeId(resumeId));
    }

    @PostMapping
    public ResponseEntity<?> add(@PathVariable Integer resumeId,
                                 @Valid @RequestBody ResumeSkillRequest request) {
        return ResponseFactory.success(resumeSkillService.add(resumeId, request));
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<?> remove(@PathVariable Integer resumeId,
                                    @PathVariable Integer skillId) {
        resumeSkillService.remove(resumeId, skillId);
        return ResponseFactory.successMessage("Skill removed from resume");
    }
}
