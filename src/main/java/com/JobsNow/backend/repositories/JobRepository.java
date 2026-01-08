package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer> {
    List<Job> findByCompany_CompanyId(Integer companyId);

    @Query("SELECT j FROM Job j WHERE j.category.id = :categoryId AND j.isActive = true AND j.isDeleted = false")
    List<Job> findByCategoryId(@Param("categoryId") Integer categoryId);

    List<Job> findByIsActiveTrueAndIsDeletedFalse();
    @Query("SELECT j FROM Job j " +
            "LEFT JOIN j.company c " +
            "WHERE j.isDeleted = false AND j.isActive = true " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:location IS NULL OR j.location IN :location) " +
            "AND (:categoryId IS NULL OR j.category.id = :categoryId)")
    List<Job> searchJobs(
            @Param("keyword") String keyword,
            @Param("location") List<String> location,
            @Param("categoryId") Integer categoryId
    );
}
