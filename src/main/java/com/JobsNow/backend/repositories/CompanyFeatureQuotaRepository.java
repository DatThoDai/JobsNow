package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.CompanyFeatureQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyFeatureQuotaRepository extends JpaRepository<CompanyFeatureQuota, Integer> {
    Optional<CompanyFeatureQuota> findByCompany_CompanyId(Integer companyId);

    @Query("SELECT q FROM CompanyFeatureQuota q WHERE q.priorityLevel >= :minLevel AND (q.expiresAt IS NULL OR q.expiresAt > CURRENT_TIMESTAMP)")
    List<CompanyFeatureQuota> findActiveByMinPriorityLevel(@Param("minLevel") int minLevel);
}
