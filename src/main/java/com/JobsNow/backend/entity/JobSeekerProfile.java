package com.JobsNow.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobSeekerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String avatarUrl;

    private String bio;

    private String address;

    private LocalDate dob;

    @OneToMany(mappedBy = "jobSeekerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    List<JobSeekerSkill> jobSeekerSkills;

    @OneToMany(mappedBy = "jobSeekerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Resume> resumes;

    @OneToMany(mappedBy = "jobSeekerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Application> applications;
}
