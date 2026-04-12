package com.JobsNow.backend.service;

import com.JobsNow.backend.request.GenerateCVRequest;
import com.JobsNow.backend.request.ImproveCVRequest;
import com.JobsNow.backend.response.GenerateCVResponse;
import com.JobsNow.backend.response.ImproveCVResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AICVService {
    ImproveCVResponse improveCVFromRequest(ImproveCVRequest request);

    ImproveCVResponse improveCVFromFile(MultipartFile file, String language);

    GenerateCVResponse generateCV(GenerateCVRequest request);
}