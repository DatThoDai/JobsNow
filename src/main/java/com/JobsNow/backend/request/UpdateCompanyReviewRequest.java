package com.JobsNow.backend.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCompanyReviewRequest {
    private Integer reviewId;
    private Integer rating;
    private String reviewText;
}
