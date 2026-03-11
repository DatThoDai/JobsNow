package com.JobsNow.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${aws.s3.endpointUrl}")
    private String endpointURL;

    @Retryable(value = { S3Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String uploadFileToS3(InputStream inputStream, String s3Key, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));

            return endpointURL + s3Key;
        } catch (Exception e) {
            log.error("AWS S3 upload error - bucket: {}, key: {}, error: {}", bucketName, s3Key, e.getMessage(), e);
            throw new RuntimeException("Upload file failed", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                System.err.println("Error closing input stream: " + ex.getMessage());
            }
        }
    }
    public String generatePreSignedUrl(String fileName, String contentType) {
        int extIndex = fileName.lastIndexOf(".");
        String baseName = fileName.substring(0, extIndex);
        String extension = fileName.substring(extIndex);
        String newFileName = baseName + "_" + System.currentTimeMillis() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(newFileName)
                .contentType(contentType)
                .build();
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(3))
                .putObjectRequest(putObjectRequest));
        return presignedRequest.url().toString();
    }
}
