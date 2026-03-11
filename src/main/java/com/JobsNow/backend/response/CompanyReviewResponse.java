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
public class CompanyReviewResponse {
    private Integer reviewId;
    private Integer rating;
    private String reviewText;
    private LocalDateTime reviewDate;
    private Integer companyId;
    private Integer jobSeekerProfileId;
    private String jobSeekerProfileName;
}
