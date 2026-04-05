package com.JobsNow.backend.repositories;

import com.JobsNow.backend.dto.ResumeDTO;
import com.JobsNow.backend.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Integer> {
    boolean existsByResumeNameAndJobSeekerProfile_ProfileId(String resumeName, Integer profileId);
    List<Resume> findByJobSeekerProfile_ProfileIdAndIsDeletedFalse(Integer profileId);
    Optional<Resume> findFirstByJobSeekerProfile_ProfileIdAndIsDeletedFalseAndIsPrimaryTrue(Integer profileId);
}
