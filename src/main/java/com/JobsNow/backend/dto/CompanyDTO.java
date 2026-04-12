package com.JobsNow.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDTO {
    private Integer companyId;
    private Integer ownerUserId;
    private String companyName;
    private String logoUrl;
    private String bannerUrl;
    private String slogan;
    private String website;
    private String description;
    private String address;
    private String nameUserContact;
    private String tutorialApply;
    private String companySize;
    private List<Integer> industryIds;
    private List<IndustryDTO> industries;
    private Boolean isVerified;
    private Integer jobPostCount;
    private Long followerCount;

    private String email;
    private String phone;

    private List<CompanyImageDTO> images;

    private List<SocialDTO> socials;
}
