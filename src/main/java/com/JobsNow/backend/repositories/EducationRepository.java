package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EducationRepository extends JpaRepository<Education, Integer> {
    List<Education> findByResume_ResumeIdOrderBySortOrderAsc(Integer resumeId);
}
