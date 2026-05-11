package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.SkillDTO;
import com.JobsNow.backend.entity.Skill;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.repositories.SkillRepository;
import com.JobsNow.backend.request.CreateSkillRequest;
import com.JobsNow.backend.response.PagedResponse;
import com.JobsNow.backend.service.SkillService;
import com.JobsNow.backend.util.PagingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {
    private final SkillRepository skillRepository;
    @Override
    public List<SkillDTO> getAllSkills() {
        List<Skill> skills = skillRepository.findAll();
        return skills.stream()
                .map(skill -> new SkillDTO(skill.getSkillId(), skill.getSkillName()))
                .collect(Collectors.toList());
    }

    @Override
    public PagedResponse<SkillDTO> getSkillsPage(int page, int limit) {
        int p = PagingUtil.safePage(page);
        int lim = PagingUtil.safeLimit(limit, 100);
        Page<Skill> pg = skillRepository.findAllByOrderBySkillNameAsc(PageRequest.of(p - 1, lim));
        return PagedResponse.<SkillDTO>builder()
                .items(pg.getContent().stream()
                        .map(skill -> new SkillDTO(skill.getSkillId(), skill.getSkillName()))
                        .toList())
                .totalCount(pg.getTotalElements())
                .page(p)
                .limit(lim)
                .hasNext(pg.hasNext())
                .build();
    }

    @Override
    public void addSkill(CreateSkillRequest request) {
        if(skillRepository.existsBySkillName(request.getSkillName())) {
            throw new BadRequestException("Skill already exists");
        }
        Skill skill = Skill.builder()
                .skillName(request.getSkillName())
                .build();
        skillRepository.save(skill);
    }

    @Override
    public void updateSkill(Integer skillId, String skillName) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new BadRequestException("Skill not found"));
        skill.setSkillName(skillName);
        skillRepository.save(skill);
    }

    @Override
    public void deleteSkill(Integer skillId) {
        if(!skillRepository.existsById(skillId)) {
            throw new BadRequestException("Skill not found");
        }
        skillRepository.deleteById(skillId);
    }
}
