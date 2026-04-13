package com.JobsNow.backend.response;

import com.JobsNow.backend.entity.enums.CompanyPostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyPostMineResponse {
    private Integer postId;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String featuredImageUrl;
    private String categoryKey;
    private CompanyPostStatus status;
    private String rejectionNote;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
