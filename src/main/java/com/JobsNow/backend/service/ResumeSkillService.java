package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.ResumeSkillDTO;
import com.JobsNow.backend.request.ResumeSkillRequest;

import java.util.List;

public interface ResumeSkillService {
    List<ResumeSkillDTO> getByResumeId(Integer resumeId);
    ResumeSkillDTO add(Integer resumeId, ResumeSkillRequest request);
    void remove(Integer resumeId, Integer skillId);
}
