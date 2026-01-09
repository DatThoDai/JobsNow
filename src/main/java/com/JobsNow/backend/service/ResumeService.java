package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.ResumeDTO;
import com.JobsNow.backend.request.CreateResumeRequest;

import java.util.List;

public interface ResumeService {
    void createResume(Integer profileId, CreateResumeRequest request);
    void deleteResume(Integer resumeId);
    List<ResumeDTO> getResumesByProfileId(Integer profileId);
    void setPrimaryResume(Integer resumeId, Integer profileId);
}
