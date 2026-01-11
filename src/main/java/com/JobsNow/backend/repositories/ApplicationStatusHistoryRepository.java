package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory,Integer> {
    List<ApplicationStatusHistory> findByApplication_ApplicationIdOrderByChangedAtAsc(Integer applicationId);
}
