package com.JobsNow.backend.controllers;

import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("savedJob")
@RequiredArgsConstructor
public class SavedJobController {
    private final SavedJobService savedJobService;
    @PostMapping("/{profileId}/job/{jobId}")
    public ResponseEntity<?> saveJob(@PathVariable Integer profileId, @PathVariable Integer jobId) {
        return ResponseFactory.success(savedJobService.saveJob(profileId, jobId));
    }

    @DeleteMapping("/{profileId}/job/{jobId}")
    public ResponseEntity<?> unsaveJob(@PathVariable Integer profileId, @PathVariable Integer jobId) {
        savedJobService.unsaveJob(profileId, jobId);
        return ResponseFactory.successMessage("Job unsaved successfully");
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<?> getSavedJobs(@PathVariable Integer profileId) {
        return ResponseFactory.success(savedJobService.getSavedJobs(profileId));
    }

    @GetMapping("/{profileId}/job/{jobId}")
    public ResponseEntity<?> isJobSaved(@PathVariable Integer profileId, @PathVariable Integer jobId) {
        return ResponseFactory.success(savedJobService.isJobSaved(profileId, jobId));
    }
}
