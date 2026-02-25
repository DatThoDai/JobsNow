package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.CompanyImage;
import com.JobsNow.backend.entity.enums.CompanyImageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyImageRepository extends JpaRepository<CompanyImage, Integer> {
    List<CompanyImage> findByCompany_CompanyId(Integer companyId);
    List<CompanyImage> findByCompany_CompanyIdAndImageType(Integer companyId, CompanyImageType imageType);
    int countByCompany_CompanyId(Integer companyId);
}
