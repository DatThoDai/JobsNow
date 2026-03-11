package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.WorkExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkExperienceRepository extends JpaRepository<WorkExperience, Integer> {
    List<WorkExperience> findByResume_ResumeIdOrderBySortOrderAsc(Integer resumeId);
}
