package com.JobsNow.backend.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddCompanyReviewRequest {
    private Integer companyId;
    private Integer jobSeekerId;
    private String review;
    private Integer rating;
}
