package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.CompanyDTO;
import com.JobsNow.backend.dto.CompanyImageDTO;
import com.JobsNow.backend.entity.Company;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class CompanyMapper {
    public static CompanyDTO toCompanyDTO(Company company) {
        return CompanyDTO.builder()
                .companyId(company.getCompanyId())
                .companyName(company.getCompanyName())
                .logoUrl(company.getLogoUrl())
                .bannerUrl(company.getBannerUrl())
                .slogan(company.getSlogan())
                .website(company.getWebsite())
                .description(company.getDescription())
                .isVerified(company.getIsVerified())
                .jobPostCount(company.getJobPostCount())
                .email(company.getUser().getEmail())
                .phone(company.getUser().getPhone())
                .images(company.getImages() != null
                        ? company.getImages().stream()
                        .map(img -> CompanyImageDTO.builder()
                                .imageId(img.getImageId())
                                .imageUrl(img.getImageUrl())
                                .type(img.getImageType().name())
                                .build())
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }
}
