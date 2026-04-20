package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.CompanyReview;
import com.JobsNow.backend.entity.enums.CompanyReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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
    long countByCompanyCompanyIdAndStatusAndCreatedAtBetween(
            Integer companyId,
            CompanyReviewStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

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

    @Query("""
        select r.rating, count(r)
        from CompanyReview r
        where r.company.companyId = :companyId
          and r.status = :status
          and r.createdAt between :start and :end
        group by r.rating
    """)
    List<Object[]> countByRatingInRange(
            @Param("companyId") Integer companyId,
            @Param("status") CompanyReviewStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        select coalesce(avg(r.rating), 0)
        from CompanyReview r
        where r.company.companyId = :companyId
          and r.status = :status
          and r.createdAt between :start and :end
    """)
    Double getAverageRatingByCompanyIdAndStatusAndCreatedAtBetween(
            @Param("companyId") Integer companyId,
            @Param("status") CompanyReviewStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<CompanyReview> findByCompanyCompanyIdAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
            Integer companyId,
            CompanyReviewStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}
