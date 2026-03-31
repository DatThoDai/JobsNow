package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.CreateCompanyReviewRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.CompanyReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyReviewController {
    private final CompanyReviewService companyReviewService;

    @GetMapping("/{companyId}/reviews")
    public ResponseEntity<?> getApprovedReviews(
            @PathVariable Integer companyId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseFactory.success(companyReviewService.getApprovedReviews(companyId, page, limit));
    }

    @PostMapping("/{companyId}/reviews")
    public ResponseEntity<?> createReview(
            Authentication auth,
            @PathVariable Integer companyId,
            @RequestBody CreateCompanyReviewRequest request
    ) {
        companyReviewService.createReview(companyId, auth.getName(), request);
        return ResponseFactory.successMessage("Review submitted, waiting for approval");
    }

    @GetMapping("/recruiter/reviews/pending")
    public ResponseEntity<?> getRecruiterPendingReviews(
            Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseFactory.success(companyReviewService.getMyCompanyPendingReviews(auth.getName(), page, limit));
    }

    @PutMapping("/recruiter/reviews/{reviewId}/approve")
    public ResponseEntity<?> approveReview(Authentication auth, @PathVariable Integer reviewId) {
        companyReviewService.approveReview(reviewId, auth.getName());
        return ResponseFactory.successMessage("Review approved");
    }

    @PutMapping("/recruiter/reviews/{reviewId}/reject")
    public ResponseEntity<?> rejectReview(Authentication auth, @PathVariable Integer reviewId) {
        companyReviewService.rejectReview(reviewId, auth.getName());
        return ResponseFactory.successMessage("Review rejected");
    }
}
