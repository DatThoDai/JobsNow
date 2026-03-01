package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
    Optional<Company> findByUser_UserId(Integer userId);
    boolean existsByCompanyName(String companyName);
    @Query("SELECT DISTINCT c " +
            "FROM Company c " +
            "LEFT JOIN c.industries i " +
            "WHERE (:industryId IS NULL OR i.industryId = :industryId) " +
            "AND (:companyName IS NULL " +
            "OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :companyName, '%')))")
    List<Company> findByIndustryOrCompanyName(
            @Param("industryId") Integer industryId,
            @Param("companyName") String companyName
    );

    @Query("SELECT COUNT(c) > 0 FROM Company c JOIN c.industries i WHERE i.industryId = :industryId")
    boolean existsByIndustryId(@Param("industryId") Integer industryId);
}
