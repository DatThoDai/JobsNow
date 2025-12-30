package com.JobNexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "job_seeker_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobSeekerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Integer profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    private String avatarUrl;

    @Column(columnDefinition = "LONGTEXT")
    private String bio;

    private String address;

    private LocalDate dob;
}
