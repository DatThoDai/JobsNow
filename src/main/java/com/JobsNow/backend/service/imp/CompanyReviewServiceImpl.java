package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.CompanyReview;
import com.JobsNow.backend.entity.JobSeekerProfile;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.entity.enums.CompanyReviewStatus;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.CompanyReviewRepository;
import com.JobsNow.backend.repositories.JobSeekerProfileRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.request.CreateCompanyReviewRequest;
import com.JobsNow.backend.response.CompanyReviewItemResponse;
import com.JobsNow.backend.response.CompanyReviewListResponse;
import com.JobsNow.backend.service.CompanyReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompanyReviewServiceImpl implements CompanyReviewService {
    private final CompanyReviewRepository companyReviewRepository;
    private final CompanyRepository companyRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final UserRepository userRepository;

    @Override
    public void createReview(Integer companyId, String email, CreateCompanyReviewRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        JobSeekerProfile profile = jobSeekerProfileRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new NotFoundException("Job seeker profile not found"));

        if (companyReviewRepository.existsByCompanyCompanyIdAndJobSeekerProfileProfileId(companyId, profile.getProfileId())) {
            throw new BadRequestException("You already reviewed this company");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Review title is required");
        }
        if (request.getPros() == null || request.getPros().isBlank()) {
            throw new BadRequestException("Review pros is required");
        }
        if (request.getCons() == null || request.getCons().isBlank()) {
            throw new BadRequestException("Review cons is required");
        }

        CompanyReview review = CompanyReview.builder()
                .company(company)
                .jobSeekerProfile(profile)
                .rating(request.getRating())
                .title(request.getTitle().trim())
                .pros(request.getPros().trim())
                .cons(request.getCons().trim())
                .recommend(Boolean.TRUE.equals(request.getRecommend()))
                .status(CompanyReviewStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        companyReviewRepository.save(review);
    }

    @Override
    public CompanyReviewListResponse getApprovedReviews(Integer companyId, int page, int limit) {
        companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);

        Page<CompanyReview> reviewPage = companyReviewRepository.findByCompanyCompanyIdAndStatusOrderByCreatedAtDesc(
                companyId,
                CompanyReviewStatus.APPROVED,
                PageRequest.of(safePage - 1, safeLimit)
        );

        Double avgRating = companyReviewRepository.getAverageRatingByCompanyIdAndStatus(
                companyId,
                CompanyReviewStatus.APPROVED
        );
        long totalCount = companyReviewRepository.countByCompanyCompanyIdAndStatus(companyId, CompanyReviewStatus.APPROVED);

        return CompanyReviewListResponse.builder()
                .items(reviewPage.getContent().stream().map(this::toItemResponse).toList())
                .totalCount(totalCount)
                .averageRating(avgRating == null ? 0d : avgRating)
                .page(safePage)
                .limit(safeLimit)
                .hasNext(reviewPage.hasNext())
                .build();
    }

    @Override
    public CompanyReviewListResponse getPendingReviewsForAdmin(int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);

        Page<CompanyReview> reviewPage = companyReviewRepository.findByStatusOrderByCreatedAtDesc(
                CompanyReviewStatus.PENDING,
                PageRequest.of(safePage - 1, safeLimit)
        );

        long totalCount = companyReviewRepository.countByStatus(CompanyReviewStatus.PENDING);

        return CompanyReviewListResponse.builder()
                .items(reviewPage.getContent().stream().map(this::toItemResponse).toList())
                .totalCount(totalCount)
                .averageRating(0d)
                .page(safePage)
                .limit(safeLimit)
                .hasNext(reviewPage.hasNext())
                .build();
    }

    @Override
    public void approveReviewByAdmin(Integer reviewId) {
        CompanyReview review = companyReviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        review.setStatus(CompanyReviewStatus.APPROVED);
        review.setUpdatedAt(LocalDateTime.now());
        companyReviewRepository.save(review);
    }

    @Override
    public void rejectReviewByAdmin(Integer reviewId) {
        CompanyReview review = companyReviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        review.setStatus(CompanyReviewStatus.REJECTED);
        review.setUpdatedAt(LocalDateTime.now());
        companyReviewRepository.save(review);
    }

    private CompanyReviewItemResponse toItemResponse(CompanyReview review) {
        return CompanyReviewItemResponse.builder()
                .reviewId(review.getReviewId())
                .userName(review.getJobSeekerProfile() != null && review.getJobSeekerProfile().getUser() != null
                        ? review.getJobSeekerProfile().getUser().getFullName()
                        : "Anonymous")
                .rating(review.getRating())
                .title(review.getTitle())
                .pros(review.getPros())
                .cons(review.getCons())
                .recommend(Boolean.TRUE.equals(review.getRecommend()))
                .createdAt(review.getCreatedAt())
                .build();
    }
}
