package com.JobsNow.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface CVParserService {
    String extractText(MultipartFile file);
}