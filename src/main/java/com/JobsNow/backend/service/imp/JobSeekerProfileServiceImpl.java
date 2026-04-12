package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.JobSeekerProfileDTO;
import com.JobsNow.backend.entity.JobSeekerProfile;
import com.JobsNow.backend.entity.JobSeekerSkill;
import com.JobsNow.backend.entity.JobSeekerSkillId;
import com.JobsNow.backend.entity.Skill;
import com.JobsNow.backend.entity.Social;
import com.JobsNow.backend.entity.enums.SocialPlatform;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.JobSeekerProfileMapper;
import com.JobsNow.backend.repositories.JobSeekerProfileRepository;
import com.JobsNow.backend.repositories.JobSeekerSkillRepository;
import com.JobsNow.backend.repositories.SkillRepository;
import com.JobsNow.backend.request.SocialLinkItem;
import com.JobsNow.backend.request.UpdateJobSeekerSkillsRequest;
import com.JobsNow.backend.request.UpdateProfileRequest;
import com.JobsNow.backend.service.AwsS3Service;
import com.JobsNow.backend.service.JobSeekerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobSeekerProfileServiceImpl implements JobSeekerProfileService {
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final AwsS3Service awsS3Service;
    private final JobSeekerSkillRepository jobSeekerSkillRepository;
    private final SkillRepository skillRepository;
    @Override
    @Transactional(readOnly = true)
    public List<JobSeekerProfileDTO> getAllJobSeekerProfiles() {
        List<JobSeekerProfile> profile = jobSeekerProfileRepository.findAll();
        return profile.stream()
                .map(JobSeekerProfileMapper::toJobSeekerProfileDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public JobSeekerProfileDTO getProfileById(Integer profileId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        return JobSeekerProfileMapper.toJobSeekerProfileDTO(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public JobSeekerProfileDTO getProfileByUserId(Integer userId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        return JobSeekerProfileMapper.toJobSeekerProfileDTO(profile);
    }

    @Override
    @Transactional
    public void updateProfile(Integer profileId, UpdateProfileRequest request) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        if(request.getBio()!=null){
            profile.setBio(request.getBio());
        }
        if(request.getFullName()!=null){
            profile.getUser().setFullName(request.getFullName());
        }
        if(request.getPhone()!=null){
            profile.getUser().setPhone(request.getPhone());
        }
        if(request.getTitle()!=null){
            profile.setTitle(request.getTitle());
        }
        if(request.getAddress()!=null){
            profile.setAddress(request.getAddress());
        }
        if(request.getDob()!=null){
            profile.setDob(request.getDob());
        }
        replaceProfileSocialsFromRequest(profile, request.getSocials());
        jobSeekerProfileRepository.save(profile);
    }

    private void replaceProfileSocialsFromRequest(JobSeekerProfile profile, List<SocialLinkItem> items) {
        if (items == null) {
            return;
        }
        if (profile.getSocials() == null) {
            profile.setSocials(new ArrayList<>());
        } else {
            profile.getSocials().clear();
        }
        for (SocialLinkItem item : items) {
            if (item.getUrl() == null || item.getUrl().isBlank()) {
                continue;
            }
            SocialPlatform platform;
            try {
                platform = SocialPlatform.valueOf(item.getPlatform().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid social platform: " + item.getPlatform());
            }
            Social s = new Social();
            s.setPlatform(platform);
            s.setUrl(item.getUrl().trim());
            s.setLogoUrl(item.getLogoUrl());
            s.setJobSeekerProfile(profile);
            profile.getSocials().add(s);
        }
    }

    @Override
    public void uploadAvatar(Integer profileId, MultipartFile avatarFile) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        if(avatarFile==null || avatarFile.isEmpty()){
            throw new BadRequestException("Avatar file is required");
        }
        try {
            String originalFileName = avatarFile.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String baseName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
            String s3Key = "avatars/" + baseName + "_" + System.currentTimeMillis() + extension;
            String avatarUrl = awsS3Service.uploadFileToS3(avatarFile.getInputStream(), s3Key, avatarFile.getContentType());
            profile.setAvatarUrl(avatarUrl);
            jobSeekerProfileRepository.save(profile);
        }catch (Exception e) {
            throw new BadRequestException("Failed to upload avatar: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateSkills(UpdateJobSeekerSkillsRequest request) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        jobSeekerSkillRepository.deleteByJobSeekerProfile_ProfileId(request.getProfileId());
        if(request.getSkills()!=null && !request.getSkills().isEmpty()){
            List<JobSeekerSkill> newSkill = new ArrayList<>();
            for(UpdateJobSeekerSkillsRequest.SkillItem skillItem : request.getSkills()){
                Skill skill = skillRepository.findById(skillItem.getSkillId())
                        .orElseThrow(() -> new NotFoundException("Skill not found"));
                JobSeekerSkillId id = new JobSeekerSkillId(request.getProfileId(), skillItem.getSkillId());
                JobSeekerSkill jobSeekerSkill = JobSeekerSkill.builder()
                        .id(id)
                        .jobSeekerProfile(profile)
                        .skill(skill)
                        .level(skillItem.getLevel())
                        .yearsOfExperience(skillItem.getYearsOfExperience())
                        .build();
                newSkill.add(jobSeekerSkill);
            }
            jobSeekerSkillRepository.saveAll(newSkill);
        }
    }
}
