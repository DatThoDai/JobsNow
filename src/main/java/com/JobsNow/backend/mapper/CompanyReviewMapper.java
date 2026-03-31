package com.JobsNow.backend.mapper;

import com.JobsNow.backend.entity.CompanyReview;
import com.JobsNow.backend.response.CompanyReviewResponse;
import org.springframework.stereotype.Component;

@Component
public class CompanyReviewMapper {
    public static CompanyReviewResponse toResponse(CompanyReview review) {
        return CompanyReviewResponse.builder()
                .reviewId(review.getReviewId())
                .userName(review.getJobSeekerProfile() != null && review.getJobSeekerProfile().getUser() != null
                        ? review.getJobSeekerProfile().getUser().getFullName()
                        : "Anonymous")
                .rating(review.getRating())
                .title(review.getTitle())
                .pros(review.getPros())
                .cons(review.getCons())
                .recommend(review.getRecommend())
                .status(review.getStatus() == null ? null : review.getStatus().name())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
