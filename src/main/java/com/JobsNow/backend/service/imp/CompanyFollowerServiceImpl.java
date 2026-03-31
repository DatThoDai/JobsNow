package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.CompanyFollower;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.repositories.CompanyFollowerRepository;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.service.CompanyFollowerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompanyFollowerServiceImpl implements CompanyFollowerService {
    private final CompanyFollowerRepository companyFollowerRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Override
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
    public void unfollowCompany(Integer companyId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        companyFollowerRepository.deleteByCompanyCompanyIdAndUserUserId(companyId, user.getUserId());
    }

    @Override
    public boolean isFollowing(Integer companyId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return companyFollowerRepository.existsByCompanyCompanyIdAndUserUserId(companyId, user.getUserId());
    }
}
