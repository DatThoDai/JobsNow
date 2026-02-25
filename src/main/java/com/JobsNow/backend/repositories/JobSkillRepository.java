package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.JobSkill;
import com.JobsNow.backend.entity.JobSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobSkillRepository extends JpaRepository<JobSkill, JobSkillId> {
    List<JobSkill> findByJob_JobId(Integer jobId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM JobSkill js WHERE js.job.jobId = :jobId")
    void deleteByJobId(Integer jobId);

}
