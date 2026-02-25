package com.JobsNow.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_job", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"job_seeker_profile_id", "job_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer savedJobId;

    private LocalDateTime savedAt;

    @ManyToOne
    @JoinColumn(name = "job_seeker_profile_id")
    private JobSeekerProfile jobSeekerProfile;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @PrePersist
    protected void onCreate() {
        savedAt = LocalDateTime.now();
    }
}
