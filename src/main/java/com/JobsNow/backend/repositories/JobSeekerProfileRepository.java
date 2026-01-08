package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.JobSeekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobSeekerProfileRepository extends JpaRepository<JobSeekerProfile,Integer> {
    Optional<JobSeekerProfile> findByUser_UserId(Integer userId);
}
