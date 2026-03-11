package com.JobsNow.backend.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeSkillRequest {
    @NotNull(message = "Skill ID is required")
    private Integer skillId;
    private String level;
}
