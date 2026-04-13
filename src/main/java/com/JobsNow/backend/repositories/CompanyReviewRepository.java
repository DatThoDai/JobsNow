package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.CompanyReview;
import com.JobsNow.backend.entity.enums.CompanyReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompanyReviewRepository extends JpaRepository<CompanyReview, Integer> {
    Page<CompanyReview> findByCompanyCompanyIdAndStatusOrderByCreatedAtDesc(
            Integer companyId,
            CompanyReviewStatus status,
            Pageable pageable
    );
    Page<CompanyReview> findByStatusOrderByCreatedAtDesc(
            CompanyReviewStatus status,
            Pageable pageable
    );

    long countByCompanyCompanyIdAndStatus(Integer companyId, CompanyReviewStatus status);
    long countByStatus(CompanyReviewStatus status);

    @Query("""
        select coalesce(avg(r.rating), 0)
        from CompanyReview r
        where r.company.companyId = :companyId and r.status = :status
    """)
    Double getAverageRatingByCompanyIdAndStatus(
            @Param("companyId") Integer companyId,
            @Param("status") CompanyReviewStatus status
    );

    boolean existsByCompanyCompanyIdAndJobSeekerProfileProfileId(Integer companyId, Integer profileId);

    Optional<CompanyReview> findByCompanyCompanyIdAndJobSeekerProfileProfileId(Integer companyId, Integer profileId);
}
