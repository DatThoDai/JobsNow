package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    @Query("""
        select count(c)
        from Company c
        where coalesce(c.createdAt, c.user.createdAt) between :start and :end
    """)
    long countCreatedInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        select coalesce(c.createdAt, c.user.createdAt)
        from Company c
        where coalesce(c.createdAt, c.user.createdAt) between :start and :end
        order by coalesce(c.createdAt, c.user.createdAt) asc
    """)
    List<LocalDateTime> findCreatedAtValuesInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
