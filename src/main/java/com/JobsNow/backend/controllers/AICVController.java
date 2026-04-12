package com.JobsNow.backend.controllers;

import com.JobsNow.backend.entity.JobMatchScore;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.repositories.JobMatchScoreRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.request.GenerateCVRequest;
import com.JobsNow.backend.request.ImproveCVRequest;
import com.JobsNow.backend.request.JobMatchRequest;
import com.JobsNow.backend.mapper.JobMatchScoreMapper;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.AICVService;
import com.JobsNow.backend.service.CandidateQuotaService;
import com.JobsNow.backend.service.CompanyQuotaService;
import com.JobsNow.backend.service.JobMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AICVController {
    private final AICVService aiCVService;
    private final JobMatchService jobMatchService;
    private final JobMatchScoreRepository jobMatchScoreRepository;
    private final CompanyQuotaService companyQuotaService;
    private final CandidateQuotaService candidateQuotaService;
    private final UserRepository userRepository;

    @PostMapping("/improve-cv")
    public ResponseEntity<?> improveCVFromText(@RequestBody ImproveCVRequest request, Authentication authentication) {
        if (isCompany(authentication)) {
            consumeAiScanIfCompany(authentication);
        } else if (isJobSeeker(authentication)) {
            Integer userId = getUserId(authentication);
            candidateQuotaService.assertAiCvBuilderEnabledForCandidateUser(userId);
            candidateQuotaService.consumeAiCvBuilderForCandidateUser(userId);
        }
        return ResponseFactory.success(aiCVService.improveCVFromRequest(request));
    }

    @PostMapping("/improve-cv/upload")
    public ResponseEntity<?> improveCVFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", required = false) String language,
            Authentication authentication) {
        if (isCompany(authentication)) {
            consumeAiScanIfCompany(authentication);
        } else if (isJobSeeker(authentication)) {
            Integer userId = getUserId(authentication);
            candidateQuotaService.assertAiCvBuilderEnabledForCandidateUser(userId);
            candidateQuotaService.consumeAiCvBuilderForCandidateUser(userId);
        }
        return ResponseFactory.success(aiCVService.improveCVFromFile(file, language));
    }

    @PostMapping("/generate-cv")
    public ResponseEntity<?> generateCV(@RequestBody GenerateCVRequest request, Authentication authentication) {
        if (isCompany(authentication)) {
            Integer userId = getUserId(authentication);
            companyQuotaService.assertAiCvBuilderEnabledForCompanyUser(userId);
            companyQuotaService.consumeAiCvBuilderForCompanyUser(userId);
            companyQuotaService.consumeAiScanForCompanyUser(userId, 1);
        } else if (isJobSeeker(authentication)) {
            Integer userId = getUserId(authentication);
            candidateQuotaService.assertAiCvBuilderEnabledForCandidateUser(userId);
            candidateQuotaService.consumeAiCvBuilderForCandidateUser(userId);
        }
        return ResponseFactory.success(aiCVService.generateCV(request));
    }

    @PostMapping("/job-match")
    public ResponseEntity<?> jobMatch(@RequestBody JobMatchRequest request, Authentication authentication) {
        if (isCompany(authentication)) {
            consumeAiScanIfCompany(authentication);
        } else if (isJobSeeker(authentication)) {
            Integer userId = getUserId(authentication);
            candidateQuotaService.consumeAiMatchForCandidateUser(userId, 1);
        }
        return ResponseFactory.success(jobMatchService.calculateMatch(request));
    }

    @GetMapping("/job-match/my-matches/{profileId}")
    public ResponseEntity<?> getMatchedJobs(@PathVariable Integer profileId) {
        List<JobMatchScore> scores = jobMatchScoreRepository.findTop5ByProfileProfileIdAndOverallScoreGreaterThanOrderByOverallScoreDesc(profileId, 0);
        return ResponseFactory.success(scores.stream().map(JobMatchScoreMapper::toDTO).toList());
    }

    @GetMapping("/job-match/candidates/{jobId}")
    public ResponseEntity<?> getMatchedCandidates(@PathVariable Integer jobId) {
        List<JobMatchScore> scores = jobMatchScoreRepository.findTop10ByJobJobIdAndOverallScoreGreaterThanOrderByOverallScoreDesc(jobId, 0);
        return ResponseFactory.success(scores.stream().map(JobMatchScoreMapper::toDTO).toList());
    }

    @PostMapping("/job-match/recalculate/profile/{profileId}")
    public ResponseEntity<?> recalculateProfile(@PathVariable Integer profileId) {
        jobMatchService.recalculateForProfile(profileId);
        return ResponseFactory.successMessage("Recalculated match scores for profile");
    }

    @PostMapping("/job-match/recalculate/job/{jobId}")
    public ResponseEntity<?> recalculateJob(@PathVariable Integer jobId) {
        jobMatchService.recalculateForJob(jobId);
        return ResponseFactory.successMessage("Recalculated match scores for job");
    }

    private void consumeAiScanIfCompany(Authentication authentication) {
        if (!isCompany(authentication)) {
            return;
        }
        Integer userId = getUserId(authentication);
        companyQuotaService.consumeAiScanForCompanyUser(userId, 1);
    }

    private boolean isCompany(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_COMPANY".equals(a.getAuthority()));
    }

    private boolean isJobSeeker(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_JOBSEEKER".equals(a.getAuthority()));
    }

    private Integer getUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return user.getUserId();
    }
}
