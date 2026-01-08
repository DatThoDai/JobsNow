package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory, Integer> {
    List<JobCategory> findByIndustryId(Integer industryId);
    boolean existsByName(String name);
}
