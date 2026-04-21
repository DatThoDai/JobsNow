package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.JobViewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JobViewEventRepository extends JpaRepository<JobViewEvent, Long> {
    @Query("""
        select count(e)
        from JobViewEvent e
        where e.job.company.companyId = :companyId
          and e.viewedAt between :start and :end
    """)
    long countByCompanyInRange(
            @Param("companyId") Integer companyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<JobViewEvent> findByJob_Company_CompanyIdAndViewedAtBetweenOrderByViewedAtAsc(
            Integer companyId,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("""
        select e.job.jobId, count(e)
        from JobViewEvent e
        where e.job.company.companyId = :companyId
          and e.viewedAt between :start and :end
        group by e.job.jobId
    """)
    List<Object[]> countViewsByJobInRange(
            @Param("companyId") Integer companyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
