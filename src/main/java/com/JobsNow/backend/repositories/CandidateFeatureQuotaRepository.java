package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.CandidateFeatureQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateFeatureQuotaRepository extends JpaRepository<CandidateFeatureQuota, Integer> {
    Optional<CandidateFeatureQuota> findByUser_UserId(Integer userId);
}
