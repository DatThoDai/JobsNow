package com.JobsNow.backend.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSeekerProfileDTO {
    private Integer profileId;
    private Integer userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String bio;
    private String phone;
    private String address;
    private LocalDate dob;
    private List<JobSeekerSkillDTO> skills;
    private List<ResumeDTO> resumes;
}
