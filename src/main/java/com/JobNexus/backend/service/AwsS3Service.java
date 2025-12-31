package com.JobNexus.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class AwsS3Service {
    private final S3Client s3Client;

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
            throw new RuntimeException("Upload file failed", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                System.err.println("Error closing input stream: " + ex.getMessage());
            }
        }
    }
}
