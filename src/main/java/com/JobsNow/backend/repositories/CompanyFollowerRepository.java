package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.CompanyFollower;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyFollowerRepository extends JpaRepository<CompanyFollower, Integer> {
    boolean existsByCompanyCompanyIdAndUserUserId(Integer companyId, Integer userId);
    void deleteByCompanyCompanyIdAndUserUserId(Integer companyId, Integer userId);
    long countByCompanyCompanyId(Integer companyId);
}
