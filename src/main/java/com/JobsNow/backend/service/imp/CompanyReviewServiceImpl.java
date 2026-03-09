package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.CompanyReview;
import com.JobsNow.backend.entity.JobSeekerProfile;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.CompanyReviewMapper;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.CompanyReviewRepository;
import com.JobsNow.backend.repositories.JobSeekerProfileRepository;
import com.JobsNow.backend.request.AddCompanyReviewRequest;
import com.JobsNow.backend.request.UpdateCompanyReviewRequest;
import com.JobsNow.backend.response.CompanyReviewResponse;
import com.JobsNow.backend.service.CompanyReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyReviewServiceImpl implements CompanyReviewService {
    private final CompanyReviewRepository companyReviewRepository;
    private final CompanyRepository companyRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    @Override
    public void addCompanyReview(AddCompanyReviewRequest request) {
        if(companyReviewRepository.findByCompanyCompanyIdAndJobSeekerProfileProfileId(request.getCompanyId(), request.getJobSeekerId()).isPresent()){
            throw new RuntimeException("You have already reviewed this company");
        }
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new NotFoundException("Company not found"));
        JobSeekerProfile jobSeeker = jobSeekerProfileRepository.findById(request.getJobSeekerId())
                .orElseThrow(() -> new NotFoundException("Job Seeker Profile not found"));
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
        CompanyReview review = CompanyReview.builder()
                .company(company)
                .jobSeekerProfile(jobSeeker)
                .rating(request.getRating())
                .reviewText(request.getReview())
                .reviewDate(LocalDateTime.now())
                .isApproved(false)
                .build();
        companyReviewRepository.save(review);
    }

    @Override
    public void updateCompanyReview(UpdateCompanyReviewRequest request) {
        CompanyReview review = companyReviewRepository.findById(request.getReviewId())
                .orElseThrow(() -> new NotFoundException("Review not found"));
        review.setRating(request.getRating());
        review.setReviewText(request.getReviewText());
        review.setReviewDate(LocalDateTime.now());
        review.setIsApproved(false);
        companyReviewRepository.save(review);
    }

    @Override
    public void deleteCompanyReview(Integer reviewId) {
        CompanyReview review = companyReviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        companyReviewRepository.delete(review);
    }

    @Override
    public List<CompanyReviewResponse> getCompanyReviews(Integer companyId) {
        return companyReviewRepository.findByCompanyCompanyId(companyId).stream()
                .filter(CompanyReview::getIsApproved)
                .map(CompanyReviewMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void approveReview(Integer reviewId) {
        CompanyReview review = companyReviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        review.setIsApproved(true);
        companyReviewRepository.save(review);
    }
}
