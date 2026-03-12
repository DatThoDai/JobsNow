package com.JobsNow.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resume_skill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeSkill {
    @EmbeddedId
    private ResumeSkillId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("resumeId")
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;

    private String level;
}
