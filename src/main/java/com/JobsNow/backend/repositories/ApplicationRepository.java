package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Application;
import com.JobsNow.backend.entity.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    List<Application> findByJob_JobIdAndJobSeekerProfile_ProfileId(Integer jobId, Integer profileId);
    List<Application> findByJobSeekerProfile_ProfileId(Integer profileId);
    List<Application> findByJob_JobId(Integer jobId);
    List<Application> findByJob_Company_CompanyId(Integer companyId);
    List<Application> findByJob_Company_CompanyIdAndAppliedAtBetween(Integer companyId, LocalDateTime start, LocalDateTime end);

    boolean existsByJob_JobIdAndJobSeekerProfile_ProfileId(Integer jobId, Integer profileId);
    Long countByAppliedAtBetween(LocalDateTime start, LocalDateTime end);
    Long countByJob_Company_CompanyId(Integer companyId);
    Long countByJob_Company_CompanyIdAndAppliedAtBetween(Integer companyId, LocalDateTime start, LocalDateTime end);
    Long countByJob_Company_CompanyIdAndApplicationStatusAndAppliedAtBetween(
            Integer companyId,
            ApplicationStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
    List<Application> findTop5ByOrderByAppliedAtDesc();

    @Query("SELECT j.location, COUNT(a) FROM Application a " +
            "JOIN a.job j WHERE a.appliedAt BETWEEN :start AND :end " +
            "GROUP BY j.location ORDER BY COUNT(a) DESC")
    List<Object[]> countByLocationAndAppliedAtBetween(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        select a.applicationStatus, count(a)
        from Application a
        where a.job.company.companyId = :companyId
          and a.appliedAt between :start and :end
        group by a.applicationStatus
    """)
    List<Object[]> countByCompanyAndStatusInRange(
            @Param("companyId") Integer companyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        select a.job.jobId, count(a)
        from Application a
        where a.job.company.companyId = :companyId
          and a.appliedAt between :start and :end
        group by a.job.jobId
    """)
    List<Object[]> countAppliesByJobInRange(
            @Param("companyId") Integer companyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
