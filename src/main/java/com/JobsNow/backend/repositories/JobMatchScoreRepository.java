package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.JobMatchScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobMatchScoreRepository extends JpaRepository<JobMatchScore, Long> {
    List<JobMatchScore> findTop5ByProfileProfileIdAndOverallScoreGreaterThanOrderByOverallScoreDesc(Integer profileId, Integer score);
    List<JobMatchScore> findTop10ByJobJobIdAndOverallScoreGreaterThanOrderByOverallScoreDesc(Integer jobId, Integer score);
    @Modifying
    void deleteByProfileProfileId(Integer profileId);
    @Modifying
    void deleteByJobJobId(Integer jobId);
}
