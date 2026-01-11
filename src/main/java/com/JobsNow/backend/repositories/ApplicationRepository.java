package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    List<Application> findByJob_JobIdAndJobSeekerProfile_ProfileId(Integer jobId, Integer profileId);
    List<Application> findByJobSeekerProfile_ProfileId(Integer profileId);
    List<Application> findByJob_JobId(Integer jobId);

    boolean existsByJob_JobIdAndJobSeekerProfile_ProfileId(Integer jobId, Integer profileId);
}
