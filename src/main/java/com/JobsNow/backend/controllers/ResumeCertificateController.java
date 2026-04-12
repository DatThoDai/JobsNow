package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.CertificateRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resume/{resumeId}/certificates")
@RequiredArgsConstructor
public class ResumeCertificateController {

    private final CertificateService certificateService;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Integer resumeId) {
        return ResponseFactory.success(certificateService.getByResumeId(resumeId));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Integer resumeId,
                                    @Valid @RequestBody CertificateRequest request) {
        return ResponseFactory.success(certificateService.create(resumeId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer resumeId,
                                    @PathVariable Integer id,
                                    @Valid @RequestBody CertificateRequest request) {
        return ResponseFactory.success(certificateService.update(resumeId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer resumeId,
                                    @PathVariable Integer id) {
        certificateService.delete(resumeId, id);
        return ResponseFactory.successMessage("Certificate deleted successfully");
    }
}

