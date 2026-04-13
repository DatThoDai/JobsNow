package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.CompanyPost;
import com.JobsNow.backend.entity.enums.CompanyPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyPostRepository extends JpaRepository<CompanyPost, Integer> {

    Optional<CompanyPost> findBySlug(String slug);

    Optional<CompanyPost> findBySlugAndStatus(String slug, CompanyPostStatus status);

    Page<CompanyPost> findByCompany_CompanyIdAndStatusInOrderByUpdatedAtDesc(
            Integer companyId,
            List<CompanyPostStatus> statuses,
            Pageable pageable
    );

    Page<CompanyPost> findByStatusOrderByCreatedAtDesc(CompanyPostStatus status, Pageable pageable);

    Page<CompanyPost> findByStatusAndCategoryKeyOrderByPublishedAtDesc(
            CompanyPostStatus status,
            String categoryKey,
            Pageable pageable
    );

    Page<CompanyPost> findByStatusOrderByPublishedAtDesc(CompanyPostStatus status, Pageable pageable);
}
