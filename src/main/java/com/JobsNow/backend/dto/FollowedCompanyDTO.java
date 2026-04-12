package com.JobsNow.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowedCompanyDTO {
    private Integer companyId;
    private String companyName;
    private String logoUrl;
    private String address;
    private String companySize;
    private Integer jobPostCount;
    private Long followerCount;
    private LocalDateTime followedAt;
}
