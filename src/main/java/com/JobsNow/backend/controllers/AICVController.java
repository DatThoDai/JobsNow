package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.ImproveCVRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.AICVService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AICVController {
    private final AICVService aiCVService;

    /**
     * Cải thiện CV từ text hoặc resumeId
     * POST /api/ai/improve-cv
     * Body: { "cvText": "..." } hoặc { "resumeId": 1 }
     */
    @PostMapping("/improve-cv")
    public ResponseEntity<?> improveCVFromText(@RequestBody ImproveCVRequest request) {
        return ResponseFactory.success(aiCVService.improveCVFromRequest(request));
    }

    /**
     * Cải thiện CV từ file upload (PDF/DOCX)
     * POST /api/ai/improve-cv/upload
     * Body: multipart/form-data với field "file"
     */
    @PostMapping("/improve-cv/upload")
    public ResponseEntity<?> improveCVFromFile(@RequestParam("file") MultipartFile file) {
        return ResponseFactory.success(aiCVService.improveCVFromFile(file));
    }
}
