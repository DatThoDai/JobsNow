package com.JobsNow.backend.controllers;

import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.AwsS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/aws/s3")
@RequiredArgsConstructor
public class AwsS3Controller {
    private final AwsS3Service awsS3Service;

    @GetMapping("/presigned-url")
    public ResponseEntity<?> getPreSignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {
        String preSignedUrl = awsS3Service.generatePreSignedUrl(fileName, contentType);
        return ResponseFactory.success(Map.of("url", preSignedUrl));
    }

    /** Authenticated upload; returns public image URL for DB fields (job thumbnail, company images, etc.). */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new BadRequestException("Only image uploads are allowed");
        }
        try {
            String url = awsS3Service.uploadMultipartImage(file, "uploads");
            return ResponseFactory.success(Map.of("url", url));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        } catch (RuntimeException e) {
            throw new BadRequestException("Upload failed");
        }
    }
}
