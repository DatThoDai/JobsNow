package com.JobsNow.backend.mapper;

import com.JobsNow.backend.dto.CompanyDTO;
import com.JobsNow.backend.dto.CompanyImageDTO;
import com.JobsNow.backend.dto.IndustryDTO;
import com.JobsNow.backend.dto.SocialDTO;
import com.JobsNow.backend.entity.Company;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CompanyMapper {
    public static CompanyDTO toCompanyDTO(Company company) {
        List<Integer> industryIds = company.getIndustries() != null
                ? company.getIndustries().stream()
                .map(com.JobsNow.backend.entity.Industry::getIndustryId)
                .collect(Collectors.toList())
                : Collections.emptyList();
        List<IndustryDTO> industries = company.getIndustries() != null
                ? company.getIndustries().stream()
                .map(i -> new IndustryDTO(i.getIndustryId(), i.getName()))
                .collect(Collectors.toList())
                : Collections.emptyList();
        return CompanyDTO.builder()
                .companyId(company.getCompanyId())
                .ownerUserId(company.getUser() != null ? company.getUser().getUserId() : null)
                .companyName(company.getCompanyName())
                .logoUrl(company.getLogoUrl())
                .bannerUrl(company.getBannerUrl())
                .slogan(company.getSlogan())
                .website(company.getWebsite())
                .description(company.getDescription())
                .address(company.getAddress())
                .nameUserContact(company.getNameUserContact())
                .tutorialApply(company.getTutorialApply())
                .companySize(company.getCompanySize())
                .industryIds(industryIds)
                .industries(industries)
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
                .socials(company.getSocials() != null && !company.getSocials().isEmpty()
                        ? company.getSocials().stream()
                        .map(s -> SocialDTO.builder()
                                .id(s.getId())
                                .platform(s.getPlatform().name())
                                .url(s.getUrl())
                                .logoUrl(s.getLogoUrl())
                                .build())
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }
}
