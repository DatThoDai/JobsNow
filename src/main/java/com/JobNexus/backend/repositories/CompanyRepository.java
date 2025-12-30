package com.JobNexus.backend.repositories;

import com.JobNexus.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
    Optional<Company> findByUser_Id(Integer userId);
    boolean existsByCompanyName(String companyName);
}
