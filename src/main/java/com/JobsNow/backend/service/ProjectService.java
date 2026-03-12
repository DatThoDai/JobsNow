package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.ProjectDTO;
import com.JobsNow.backend.request.ProjectRequest;

import java.util.List;

public interface ProjectService {
    List<ProjectDTO> getByResumeId(Integer resumeId);
    ProjectDTO create(Integer resumeId, ProjectRequest request);
    ProjectDTO update(Integer resumeId, Integer id, ProjectRequest request);
    void delete(Integer resumeId, Integer id);
}
