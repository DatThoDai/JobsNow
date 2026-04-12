package com.JobsNow.backend.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyReviewListResponse {
    private List<CompanyReviewItemResponse> items;
    private long totalCount;
    private double averageRating;
    private int page;
    private int limit;
    private boolean hasNext;
}
