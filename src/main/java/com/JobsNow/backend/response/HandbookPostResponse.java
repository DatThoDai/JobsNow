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
public class HandbookPostResponse {
    private Integer postId;
    private String title;
    private String slug;
    private String excerpt;
    private String featuredImageUrl;
    private String categoryKey;
    private String companyName;
    private String companyLogoUrl;
    private LocalDateTime publishedAt;
}
