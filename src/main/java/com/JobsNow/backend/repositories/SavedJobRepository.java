package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, Integer> {
    List<SavedJob> findByJobSeekerProfile_ProfileIdOrderBySavedAtDesc(Integer profileId);
    Optional<SavedJob> findByJobSeekerProfile_ProfileIdAndJob_JobId(Integer profileId, Integer jobId);
    boolean existsByJobSeekerProfile_ProfileIdAndJob_JobId(Integer profileId, Integer jobId);
    void deleteByJobSeekerProfile_ProfileIdAndJob_JobId(Integer profileId, Integer jobId);
}
