package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.CompanyFollower;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface CompanyFollowerRepository extends JpaRepository<CompanyFollower, Integer> {
    boolean existsByCompanyCompanyIdAndUserUserId(Integer companyId, Integer userId);

    @Modifying(clearAutomatically = true)
    void deleteByCompanyCompanyIdAndUserUserId(Integer companyId, Integer userId);

    long countByCompanyCompanyId(Integer companyId);

    Page<CompanyFollower> findByCompany_CompanyIdOrderByCreatedAtDesc(Integer companyId, Pageable pageable);
}
