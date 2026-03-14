package com.JobsNow.backend.controllers;

import com.JobsNow.backend.dto.DashboardStatsDTO;
import com.JobsNow.backend.repositories.ApplicationRepository;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.ApplicationService;
import com.JobsNow.backend.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationService applicationService;
    private final JobService jobService;

    @GetMapping("/jobs")
    public ResponseEntity<?> getJobsForAdmin(@RequestParam(required = false) String status) {
        return ResponseFactory.success(jobService.getAllJobsForAdmin(status));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .totalApplications(applicationRepository.count())
                .openJobs(jobRepository.findAll().stream()
                        .filter(job -> job.getIsActive() && !job.getIsDeleted() && job.getIsApproved())
                        .count())
                .activeUsers(userRepository.countByIsVerifiedTrue())
                .pendingApprovals(jobRepository.countByIsApprovedFalse())
                .build();
        return ResponseFactory.success(stats);
    }

    @GetMapping("/applications/recent")
    public ResponseEntity<?> getRecentApplications() {
        return ResponseFactory.success(applicationService.getRecentApplications());
    }

    @GetMapping("/applications/trends")
    public ResponseEntity<?> getApplicationTrends(
            @RequestParam String type,
            @RequestParam(required = false) Integer month) {
        return ResponseFactory.success(applicationService.getApplicationTrends(type, month));
    }

    @GetMapping("/regions/active")
    public ResponseEntity<?> getActiveRegions(
            @RequestParam String type,
            @RequestParam(required = false) Integer month) {
        return ResponseFactory.success(applicationService.getActiveRegions(type, month));
    }

    @GetMapping("/companies/job-stats")
    public ResponseEntity<?> getCompanyJobStats(
            @RequestParam String type,
            @RequestParam(required = false) Integer month) {
        return ResponseFactory.success(applicationService.getCompanyJobStats(type, month));
    }
}
