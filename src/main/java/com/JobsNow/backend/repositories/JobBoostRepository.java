package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.JobBoost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobBoostRepository extends JpaRepository<JobBoost, Integer> {
    Optional<JobBoost> findByJob_JobIdAndIsActiveTrue(Integer jobId);
    Optional<JobBoost> findTopByJob_JobIdAndIsActiveTrueOrderByEndAtDesc(Integer jobId);
    List<JobBoost> findByIsActiveTrue();
    List<JobBoost> findByIsActiveTrueAndEndAtBefore(LocalDateTime now);
    List<JobBoost> findByOrder_OrderId(Integer orderId);
}
