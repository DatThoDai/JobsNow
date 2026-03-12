package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.ResumeSkillDTO;
import com.JobsNow.backend.entity.Resume;
import com.JobsNow.backend.entity.ResumeSkill;
import com.JobsNow.backend.entity.ResumeSkillId;
import com.JobsNow.backend.entity.Skill;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.repositories.ResumeRepository;
import com.JobsNow.backend.repositories.ResumeSkillRepository;
import com.JobsNow.backend.repositories.SkillRepository;
import com.JobsNow.backend.request.ResumeSkillRequest;
import com.JobsNow.backend.service.ResumeSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeSkillServiceImpl implements ResumeSkillService {
    private final ResumeSkillRepository resumeSkillRepository;
    private final ResumeRepository resumeRepository;
    private final SkillRepository skillRepository;

    @Override
    public List<ResumeSkillDTO> getByResumeId(Integer resumeId) {
        resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        return resumeSkillRepository.findByResume_ResumeId(resumeId)
                .stream()
                .map(rs -> ResumeSkillDTO.builder()
                        .skillId(rs.getSkill().getSkillId())
                        .skillName(rs.getSkill().getSkillName())
                        .level(rs.getLevel())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public ResumeSkillDTO add(Integer resumeId, ResumeSkillRequest request) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new NotFoundException("Skill not found"));
        ResumeSkillId id = new ResumeSkillId(resumeId, skill.getSkillId());
        if (resumeSkillRepository.existsById(id)) {
            throw new BadRequestException("Skill already added to this resume");
        }
        ResumeSkill resumeSkill = ResumeSkill.builder()
                .id(id)
                .resume(resume)
                .skill(skill)
                .level(request.getLevel())
                .build();
        resumeSkillRepository.save(resumeSkill);
        return ResumeSkillDTO.builder()
                .skillId(skill.getSkillId())
                .skillName(skill.getSkillName())
                .level(request.getLevel())
                .build();
    }

    @Override
    @Transactional
    public void remove(Integer resumeId, Integer skillId) {
        resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        ResumeSkillId id = new ResumeSkillId(resumeId, skillId);
        if (!resumeSkillRepository.existsById(id)) {
            throw new NotFoundException("Skill not found in this resume");
        }
        resumeSkillRepository.deleteById_ResumeIdAndId_SkillId(resumeId, skillId);
    }
}
