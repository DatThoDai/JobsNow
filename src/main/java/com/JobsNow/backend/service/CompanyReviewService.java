package com.JobsNow.backend.service;

import com.JobsNow.backend.request.CreateCompanyReviewRequest;
import com.JobsNow.backend.response.CompanyReviewListResponse;

public interface CompanyReviewService {
    void createReview(Integer companyId, String email, CreateCompanyReviewRequest request);
    CompanyReviewListResponse getApprovedReviews(Integer companyId, int page, int limit);
    CompanyReviewListResponse getMyCompanyPendingReviews(String email, int page, int limit);
    void approveReview(Integer reviewId, String email);
    void rejectReview(Integer reviewId, String email);
}
