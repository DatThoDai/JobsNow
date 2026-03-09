package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.AddCompanyReviewRequest;
import com.JobsNow.backend.request.UpdateCompanyReviewRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.CompanyReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/company/review")
@RequiredArgsConstructor
public class CompanyReviewController {
    private final CompanyReviewService companyReviewService;
    @PostMapping("/add")
    public ResponseEntity<?> addReview(@RequestBody AddCompanyReviewRequest request) {
        companyReviewService.addCompanyReview(request);
        return ResponseFactory.successMessage("Company review added successfully");
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateReview(@RequestBody UpdateCompanyReviewRequest request) {
        companyReviewService.updateCompanyReview(request);
        return ResponseFactory.successMessage("Company review updated successfully");
    }

    @DeleteMapping("/delete/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable Integer reviewId) {
        companyReviewService.deleteCompanyReview(reviewId);
        return ResponseFactory.successMessage("Company review deleted successfully");
    }

    @GetMapping("/list/{companyId}")
    public ResponseEntity<?> getReviews(@PathVariable Integer companyId) {
        return ResponseFactory.success(companyReviewService.getCompanyReviews(companyId));
    }

    @PutMapping("/approve/{reviewId}")
    public ResponseEntity<?> approveReview(@PathVariable Integer reviewId) {
        companyReviewService.approveReview(reviewId);
        return ResponseFactory.successMessage("Review approved");
    }

}
