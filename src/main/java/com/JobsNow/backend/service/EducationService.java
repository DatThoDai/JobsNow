package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.EducationDTO;
import com.JobsNow.backend.request.EducationRequest;

import java.util.List;

public interface EducationService {
    List<EducationDTO> getByResumeId(Integer resumeId);
    EducationDTO create(Integer resumeId, EducationRequest request);
    EducationDTO update(Integer resumeId, Integer id, EducationRequest request);
    void delete(Integer resumeId, Integer id);
}
