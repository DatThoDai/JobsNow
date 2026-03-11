package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.JobSeekerProfileDTO;
import com.JobsNow.backend.dto.JobSeekerSkillDTO;
import com.JobsNow.backend.entity.JobSeekerProfile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JobSeekerProfileMapper {
    public static JobSeekerProfileDTO toJobSeekerProfileDTO(JobSeekerProfile profile) {
        return JobSeekerProfileDTO.builder()
                .profileId(profile.getProfileId())
                .userId(profile.getUser().getUserId())
                .fullName(profile.getUser().getFullName())
                .email(profile.getUser().getEmail())
                .avatarUrl(profile.getAvatarUrl())
                .title(profile.getTitle())
                .bio(profile.getBio())
                .phone(profile.getUser().getPhone())
                .address(profile.getAddress())
                .dob(profile.getDob())
                .skills(profile.getJobSeekerSkills() == null ? List.of()
                        : profile.getJobSeekerSkills().stream()
                        .map(js -> JobSeekerSkillDTO.builder()
                                .skillId(js.getSkill().getSkillId())
                                .skillName(js.getSkill().getSkillName())
                                .level(js.getLevel())
                                .yearsOfExperience(js.getYearsOfExperience())
                                .build())
                        .collect(Collectors.toList()))
                .resumes(profile.getResumes() == null ? List.of()
                        : profile.getResumes().stream()
                        .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                        .map(ResumeMapper::toResumeDTO)
                        .collect(Collectors.toList()))
                .build();
    }
}
