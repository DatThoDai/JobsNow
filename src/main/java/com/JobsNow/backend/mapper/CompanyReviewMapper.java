package com.JobsNow.backend.mapper;

import com.JobsNow.backend.entity.CompanyReview;
import com.JobsNow.backend.response.CompanyReviewResponse;
import org.springframework.stereotype.Component;

@Component
public class CompanyReviewMapper {
    public static CompanyReviewResponse toResponse(CompanyReview review) {
        return CompanyReviewResponse.builder()
                .reviewId(review.getReviewId())
                .rating(review.getRating())
                .reviewText(review.getReviewText())
                .reviewDate(review.getReviewDate())
                .companyId(review.getCompany().getCompanyId())
                .jobSeekerProfileId(review.getJobSeekerProfile().getProfileId())
                .jobSeekerProfileName(
                        review.getJobSeekerProfile().getUser().getFullName()
                )
                .build();
    }
}
