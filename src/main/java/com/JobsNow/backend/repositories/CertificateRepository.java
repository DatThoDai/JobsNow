package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, Integer> {
    List<Certificate> findByResume_ResumeIdOrderBySortOrderAsc(Integer resumeId);
}
