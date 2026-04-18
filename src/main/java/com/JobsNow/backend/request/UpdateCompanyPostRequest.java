package com.JobsNow.backend.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanyPostRequest {
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String featuredImageUrl;
    private String categoryKey;
}
