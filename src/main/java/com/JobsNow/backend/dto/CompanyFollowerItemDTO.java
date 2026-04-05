package com.JobsNow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyFollowerItemDTO {
    private Integer userId;
    private String fullName;
    private String avatarUrl;
    private LocalDateTime followedAt;
}
