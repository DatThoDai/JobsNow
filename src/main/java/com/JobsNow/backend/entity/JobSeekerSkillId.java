package com.JobsNow.backend.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class JobSeekerSkillId implements Serializable {
    private Integer profileId;
    private Integer skillId;
}
