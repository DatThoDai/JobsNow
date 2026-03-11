package com.JobsNow.backend.controllers;

import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.AwsS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
