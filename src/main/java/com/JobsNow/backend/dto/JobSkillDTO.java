package com.JobsNow.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSkillDTO {
    private Integer skillId;
    private String skillName;
    private Boolean isRequired;
    private String level;
}
