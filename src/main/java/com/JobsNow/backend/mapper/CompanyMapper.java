package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.CompanyDTO;
import com.JobsNow.backend.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {
    public CompanyDTO toCompanyDTO(Company company) {
        CompanyDTO companyDTO = new CompanyDTO();
        companyDTO.setCompanyId(company.getCompanyId());
        companyDTO.setCompanyName(company.getCompanyName());
        companyDTO.setLogoUrl(company.getLogoUrl());
        companyDTO.setWebsite(company.getWebsite());
        companyDTO.setDescription(company.getDescription());
        return companyDTO;
    }
}
