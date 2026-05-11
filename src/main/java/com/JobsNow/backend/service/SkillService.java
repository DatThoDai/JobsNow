package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.SkillDTO;
import com.JobsNow.backend.request.CreateSkillRequest;
import com.JobsNow.backend.response.PagedResponse;

import java.util.List;

public interface SkillService {
    List<SkillDTO> getAllSkills();

    PagedResponse<SkillDTO> getSkillsPage(int page, int limit);
    void addSkill(CreateSkillRequest addSkillRequest);
    void updateSkill(Integer skillId, String skillName);
    void deleteSkill(Integer skillId);
}
