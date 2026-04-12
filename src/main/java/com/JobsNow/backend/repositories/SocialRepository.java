package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Social;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialRepository extends JpaRepository<Social, Integer> {
    List<Social> findByCompany_CompanyId(Integer companyId);

    List<Social> findByJobSeekerProfile_ProfileId(Integer profileId);
}
