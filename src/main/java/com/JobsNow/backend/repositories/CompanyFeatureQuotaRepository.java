package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.CompanyFeatureQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyFeatureQuotaRepository extends JpaRepository<CompanyFeatureQuota, Integer> {
    Optional<CompanyFeatureQuota> findByCompany_CompanyId(Integer companyId);
}
