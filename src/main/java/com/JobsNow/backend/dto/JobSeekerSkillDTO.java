package com.JobsNow.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSeekerSkillDTO {
    private Integer skillId;
    private String skillName;
    private String level;
    private Integer yearsOfExperience;
}
