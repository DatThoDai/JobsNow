package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.ResumeDTO;
import com.JobsNow.backend.request.CreateResumeRequest;
import com.JobsNow.backend.request.InitResumeRequest;
import com.JobsNow.backend.request.UpdateResumeRequest;

import java.util.List;

public interface ResumeService {
    void createResume(Integer profileId, CreateResumeRequest request);
    ResumeDTO initResume(Integer profileId, InitResumeRequest request);
    ResumeDTO updateResume(Integer resumeId, UpdateResumeRequest request);
    void deleteResume(Integer resumeId);
    List<ResumeDTO> getResumesByProfileId(Integer profileId);
    void setPrimaryResume(Integer resumeId, Integer profileId);
}
