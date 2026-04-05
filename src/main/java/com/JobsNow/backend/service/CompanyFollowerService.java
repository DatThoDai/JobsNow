package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.CompanyFollowerItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyFollowerService {
    void followCompany(Integer companyId, String email);
    void unfollowCompany(Integer companyId, String email);
    boolean isFollowing(Integer companyId, String email);

    Page<CompanyFollowerItemDTO> getFollowersForCompanyOwner(Integer companyId, String recruiterEmail, Pageable pageable);
}
