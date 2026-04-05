package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Job;
import com.JobsNow.backend.entity.enums.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer> {
    @Query("SELECT j FROM Job j JOIN FETCH j.company c LEFT JOIN FETCH c.socials WHERE j.jobId = :id")
    Optional<Job> findByIdWithCompanyAndSocials(@Param("id") Integer id);

    List<Job> findByCompany_CompanyId(Integer companyId);

    @Query("SELECT j FROM Job j JOIN FETCH j.company c WHERE j.category.id = :categoryId "
            + "AND j.jobId <> :excludeId AND j.isActive = true AND j.isApproved = true "
            + "AND j.isDeleted = false AND j.isExpired = false ORDER BY j.postedAt DESC")
    List<Job> findRelatedByCategory(
            @Param("categoryId") Integer categoryId,
            @Param("excludeId") Integer excludeId,
            Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.category.id = :categoryId AND j.isActive = true AND j.isDeleted = false")
    List<Job> findByCategoryId(@Param("categoryId") Integer categoryId);

    List<Job> findByIsActiveTrueAndIsDeletedFalse();
    List<Job> findByIsActiveTrueAndIsDeletedFalseOrderByFinalScoreDescPostedAtDesc();
    @Query("SELECT DISTINCT j FROM Job j " +
            "LEFT JOIN j.company c " +
            "LEFT JOIN j.jobSkills js " +
            "LEFT JOIN js.skill s " +
            "WHERE j.isDeleted = false AND j.isActive = true " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(j.requirements) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.skillName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:location IS NULL OR :location = '' OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
            "AND (:jobType IS NULL OR j.jobType = :jobType) " +
            "AND (:categoryIds IS NULL OR j.category.id IN :categoryIds) " +
            "ORDER BY j.finalScore DESC NULLS LAST, j.postedAt DESC")
    List<Job> searchJobs(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("jobType") JobType jobType,
            @Param("categoryIds") List<Integer> categoryIds
    );

    Long countByIsApprovedFalse();
    @Query("SELECT j.company.companyName, COUNT(j) FROM Job j " +
            "WHERE j.postedAt BETWEEN :start AND :end " +
            "GROUP BY j.company.companyName ORDER BY COUNT(j) DESC")
    List<Object[]> countJobsByCompanyAndCreatedAtBetween(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
