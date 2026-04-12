package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.WorkExperienceDTO;
import com.JobsNow.backend.request.WorkExperienceRequest;

import java.util.List;

public interface WorkExperienceService {
    List<WorkExperienceDTO> getByResumeId(Integer resumeId);
    WorkExperienceDTO create(Integer resumeId, WorkExperienceRequest request);
    WorkExperienceDTO update(Integer resumeId, Integer id, WorkExperienceRequest request);
    void delete(Integer resumeId, Integer id);
}
