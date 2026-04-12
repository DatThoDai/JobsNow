package com.JobsNow.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeSkillDTO {
    private Integer skillId;
    private String skillName;
    private String level;
}
