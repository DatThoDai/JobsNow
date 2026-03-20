package com.JobsNow.backend.entity;

import com.JobsNow.backend.entity.enums.SocialPlatform;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "social")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Social {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialPlatform platform;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 2048)
    private String logoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_seeker_profile_id")
    private JobSeekerProfile jobSeekerProfile;
}
