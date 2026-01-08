package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.SkillDTO;
import com.JobsNow.backend.entity.Skill;
import org.springframework.stereotype.Component;

@Component
public class SkillMapper {
    public static SkillDTO toSkillDTO(Skill skill) {
        SkillDTO skillDTO = new SkillDTO();
        skillDTO.setSkillId(skill.getSkillId());
        skillDTO.setSkillName(skill.getSkillName());
        return skillDTO;
    }
}
