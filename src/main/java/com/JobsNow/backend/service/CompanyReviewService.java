package com.JobsNow.backend.service;

import com.JobsNow.backend.request.AddCompanyReviewRequest;
import com.JobsNow.backend.request.UpdateCompanyReviewRequest;
import com.JobsNow.backend.response.CompanyReviewResponse;

import java.util.List;

public interface CompanyReviewService {
    void addCompanyReview(AddCompanyReviewRequest request);
    void updateCompanyReview(UpdateCompanyReviewRequest request);
    void deleteCompanyReview(Integer reviewId);
    List<CompanyReviewResponse> getCompanyReviews(Integer companyId);
    void approveReview(Integer reviewId);
}
