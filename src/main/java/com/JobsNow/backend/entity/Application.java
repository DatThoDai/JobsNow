package com.JobsNow.backend.entity;

import com.JobsNow.backend.entity.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer applicationId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus applicationStatus;

    private LocalDateTime appliedAt;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private JobSeekerProfile jobSeekerProfile;

    @ManyToOne
    @JoinColumn(name = "resume_id")
    private Resume resume;
}
