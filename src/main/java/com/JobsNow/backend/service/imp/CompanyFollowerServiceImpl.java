package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.CompanyFollowerItemDTO;
import com.JobsNow.backend.dto.FollowedCompanyDTO;
import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.CompanyFollower;
import com.JobsNow.backend.entity.JobSeekerProfile;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.repositories.CompanyFollowerRepository;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.JobSeekerProfileRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.service.CompanyFollowerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompanyFollowerServiceImpl implements CompanyFollowerService {
    private final CompanyFollowerRepository companyFollowerRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;

    @Override
    @Transactional
    public void followCompany(Integer companyId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        if (company.getUser() != null && company.getUser().getUserId().equals(user.getUserId())) {
            throw new BadRequestException("Company cannot follow itself");
        }

        if (companyFollowerRepository.existsByCompanyCompanyIdAndUserUserId(companyId, user.getUserId())) {
            return;
        }

        CompanyFollower follower = CompanyFollower.builder()
                .company(company)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        companyFollowerRepository.save(follower);
    }

    @Override
    @Transactional
    public void unfollowCompany(Integer companyId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        companyFollowerRepository.deleteByCompanyCompanyIdAndUserUserId(companyId, user.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(Integer companyId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return companyFollowerRepository.existsByCompanyCompanyIdAndUserUserId(companyId, user.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyFollowerItemDTO> getFollowersForCompanyOwner(Integer companyId, String recruiterEmail, Pageable pageable) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Company myCompany = companyRepository.findByUser_UserId(recruiter.getUserId())
                .orElseThrow(() -> new NotFoundException("Company not found"));
        if (!myCompany.getCompanyId().equals(companyId)) {
            throw new BadRequestException("You can only view followers of your own company");
        }
        return companyFollowerRepository.findByCompany_CompanyIdOrderByCreatedAtDesc(companyId, pageable)
                .map(cf -> {
                    User u = cf.getUser();
                    String avatar = jobSeekerProfileRepository.findByUser_UserId(u.getUserId())
                            .map(JobSeekerProfile::getAvatarUrl)
                            .orElse(null);
                    return CompanyFollowerItemDTO.builder()
                            .userId(u.getUserId())
                            .fullName(u.getFullName())
                            .avatarUrl(avatar)
                            .followedAt(cf.getCreatedAt())
                            .build();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowedCompanyDTO> getMyFollowedCompanies(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return companyFollowerRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId(), pageable)
                .map(cf -> {
                    Company c = cf.getCompany();
                    return FollowedCompanyDTO.builder()
                            .companyId(c.getCompanyId())
                            .companyName(c.getCompanyName())
                            .logoUrl(c.getLogoUrl())
                            .address(c.getAddress())
                            .companySize(c.getCompanySize())
                            .jobPostCount(c.getJobPostCount())
                            .followerCount(companyFollowerRepository.countByCompanyCompanyId(c.getCompanyId()))
                            .followedAt(cf.getCreatedAt())
                            .build();
                });
    }
}
