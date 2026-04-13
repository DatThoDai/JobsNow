package com.JobsNow.backend.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyPostAdminItemResponse {
    private Integer postId;
    private Integer companyId;
    private String companyName;
    private String title;
    private String slug;
    private String excerpt;
    private String categoryKey;
    private LocalDateTime createdAt;
}
