package com.JobsNow.backend.controllers;

import com.JobsNow.backend.dto.DashboardStatsDTO;
import com.JobsNow.backend.repositories.ApplicationRepository;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.request.UpdateAdminUserRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.AdminDashboardMetricsService;
import com.JobsNow.backend.service.AdminUserService;
import com.JobsNow.backend.service.ApplicationService;
import com.JobsNow.backend.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final AdminUserService adminUserService;
    private final AdminDashboardMetricsService adminDashboardMetricsService;

    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        return ResponseFactory.success(adminUserService.listUsers());
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable Integer userId,
            @RequestBody UpdateAdminUserRequest request) {
        return ResponseFactory.success(adminUserService.updateUser(userId, request));
    }

    @GetMapping("/jobs")
    public ResponseEntity<?> getJobsForAdmin(@RequestParam(required = false) String status) {
        return ResponseFactory.success(jobService.getAllJobsForAdmin(status));
    }

    @PutMapping("/jobs/{jobId}/unpublish")
    public ResponseEntity<?> unpublishJobByAdmin(@PathVariable Integer jobId) {
        jobService.unpublishJobByAdmin(jobId);
        return ResponseFactory.successMessage("Job unpublished successfully");
    }

    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<?> deleteJobByAdmin(@PathVariable Integer jobId) {
        jobService.deleteJob(jobId);
        return ResponseFactory.successMessage("Job deleted successfully");
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

    @GetMapping("/dashboard-metrics")
    public ResponseEntity<?> getDashboardMetrics(
            @RequestParam(defaultValue = "month") String preset,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "Asia/Ho_Chi_Minh") String tz,
            @RequestParam(defaultValue = "true") boolean comparePrevious
    ) {
        return ResponseFactory.success(adminDashboardMetricsService.getMetrics(
                preset,
                from,
                to,
                tz,
                comparePrevious
        ));
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
