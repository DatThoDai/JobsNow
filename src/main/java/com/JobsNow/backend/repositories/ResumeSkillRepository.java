package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.ResumeSkill;
import com.JobsNow.backend.entity.ResumeSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, ResumeSkillId> {
    List<ResumeSkill> findByResume_ResumeId(Integer resumeId);
    void deleteById_ResumeIdAndId_SkillId(Integer resumeId, Integer skillId);
}
