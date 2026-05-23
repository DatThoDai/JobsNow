package com.JobsNow.backend.service;

import com.JobsNow.backend.entity.JobSeekerProfile;
import com.JobsNow.backend.response.CVImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface CVImportService {
    CVImportResult parseFromFile(MultipartFile file, JobSeekerProfile profile);

    CVImportResult parseFromText(String cvText, JobSeekerProfile profile);

    CVImportResult parseFromBytes(byte[] fileBytes, String fileName, JobSeekerProfile profile);
}
