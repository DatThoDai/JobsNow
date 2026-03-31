package com.JobsNow.backend.service;

public interface CompanyFollowerService {
    void followCompany(Integer companyId, String email);
    void unfollowCompany(Integer companyId, String email);
    boolean isFollowing(Integer companyId, String email);
}
