package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.constants.JobsNowConstant;
import com.JobsNow.backend.dto.ChartDataDTO;
import com.JobsNow.backend.dto.CompanyJobStatsDTO;
import com.JobsNow.backend.dto.RecentApplicationDTO;
import com.JobsNow.backend.dto.RegionChartDataDTO;
import com.JobsNow.backend.entity.*;
import com.JobsNow.backend.entity.enums.ApplicationStatus;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.ApplicationMapper;
import com.JobsNow.backend.repositories.*;
import com.JobsNow.backend.request.ApplicationRequest;
import com.JobsNow.backend.request.CreateNotificationRequest;
import com.JobsNow.backend.response.ApplicationDetailResponse;
import com.JobsNow.backend.response.ApplicationOfJobResponse;
import com.JobsNow.backend.response.NotificationResponse;
import com.JobsNow.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    private final NotificationServiceImpl notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    @Override
    @Transactional
    public void applyForJob(ApplicationRequest request) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new NotFoundException("Job not found"));
        if(!job.getIsActive()||job.getIsDeleted()||!job.getIsApproved()){
            throw new BadRequestException("Job is not available for application");
        }
        JobSeekerProfile jobSeekerProfile = jobSeekerProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new NotFoundException("Job seeker profile not found"));
        Resume resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        if(applicationRepository.existsByJob_JobIdAndJobSeekerProfile_ProfileId(request.getJobId(), request.getProfileId())){
            throw new BadRequestException("You have already applied for this job");
        }
        Application application = Application.builder()
                .job(job)
                .jobSeekerProfile(jobSeekerProfile)
                .resume(resume)
                .appliedAt(LocalDateTime.now())
                .applicationStatus(ApplicationStatus.PENDING)
                .build();
        applicationRepository.save(application);
        saveStatusHistory(application, ApplicationStatus.PENDING);
    }

    @Override
    public List<ApplicationDetailResponse> getApplicationsByJobSeeker(Integer profileId) {
        List<Application> applications = applicationRepository.findByJobSeekerProfile_ProfileId(profileId);
        if(applications.isEmpty()){
            throw new NotFoundException("No applications found for this job seeker");
        }
        return applications.stream()
                .map(app -> {
                    List<ApplicationStatusHistory> history = applicationStatusHistoryRepository.findByApplication_ApplicationIdOrderByChangedAtAsc(app.getApplicationId());
                    return ApplicationMapper.toDetailResponse(app, history);
                })
                .collect(Collectors.toList());
    }

    @Override
    public ApplicationDetailResponse getApplicationDetail(Integer applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        List<ApplicationStatusHistory> history = applicationStatusHistoryRepository.findByApplication_ApplicationIdOrderByChangedAtAsc(applicationId);
        return ApplicationMapper.toDetailResponse(application, history);
    }

    @Override
    public List<ApplicationOfJobResponse> getApplicationsByJob(Integer jobId) {
        List<Application> applications = applicationRepository.findByJob_JobId(jobId);
        if(applications.isEmpty()){
            throw new NotFoundException("No applications found for this job");
        }
        return applications.stream().map(ApplicationMapper::toApplicationOfJobResponse).collect(Collectors.toList());
    }

    @Override
    public List<ApplicationDetailResponse> getApplicationsByCompany(Integer companyId) {
        List<Application> applications = applicationRepository.findByJob_Company_CompanyId(companyId);
        return applications.stream()
                .map(app -> {
                    List<ApplicationStatusHistory> history = applicationStatusHistoryRepository.findByApplication_ApplicationIdOrderByChangedAtAsc(app.getApplicationId());
                    return ApplicationMapper.toDetailResponse(app, history);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void updateApplicationStatus(Integer applicationId, String status) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        try {
            ApplicationStatus newStatus = ApplicationStatus.valueOf(status.toUpperCase());
            application.setApplicationStatus(newStatus);
            applicationRepository.save(application);
            saveStatusHistory(application, newStatus);

            Integer jobSeekerId = application.getJobSeekerProfile().getUser().getUserId();
            CreateNotificationRequest notiRequest = CreateNotificationRequest.builder()
                    .applicationId(applicationId)
                    .userId(jobSeekerId)
                    .content(newStatus.toString())
                    .build();
            NotificationResponse notification = notificationService.createNotification(notiRequest);
            messagingTemplate.convertAndSend(
                    JobsNowConstant.WS_TOPIC_NOTIFICATION + jobSeekerId,
                    notification);
        }catch (IllegalArgumentException e){
            throw new BadRequestException("Invalid application status");
        }
    }

    @Override
    public List<RecentApplicationDTO> getRecentApplications() {
        return applicationRepository.findTop5ByOrderByAppliedAtDesc().stream()
                .map(app -> RecentApplicationDTO.builder()
                        .id(app.getApplicationId())
                        .applicant(app.getJobSeekerProfile().getUser().getFullName())
                        .jobTitle(app.getJob().getTitle())
                        .status(app.getApplicationStatus().toString())
                        .date(app.getAppliedAt().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public ChartDataDTO getApplicationTrends(String type, Integer month) {
        ChartDataDTO dto = new ChartDataDTO();
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        if ("range".equals(type)) {
            // Theo từng tháng trong năm
            List<String> labels = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            for (int m = 1; m <= now.getMonthValue(); m++) {
                LocalDateTime start = LocalDateTime.of(currentYear, m, 1, 0, 0);
                LocalDateTime end = start.plusMonths(1);
                long count = applicationRepository.countByAppliedAtBetween(start, end);
                labels.add(java.time.Month.of(m).name().substring(0, 3));
                counts.add(count);
            }
            dto.setLabels(labels);
            dto.setCounts(counts);
        } else if ("month".equals(type) && month != null) {
            // Theo từng ngày trong tháng
            java.time.YearMonth yearMonth = java.time.YearMonth.of(currentYear, month);
            List<String> labels = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
                LocalDateTime start = LocalDateTime.of(currentYear, month, day, 0, 0);
                LocalDateTime end = start.plusDays(1);
                long count = applicationRepository.countByAppliedAtBetween(start, end);
                labels.add(String.format("%02d", day));
                counts.add(count);
            }
            dto.setLabels(labels);
            dto.setCounts(counts);
        } else {
            throw new BadRequestException("Invalid type or month");
        }
        return dto;
    }

    @Override
    public RegionChartDataDTO getActiveRegions(String type, Integer month) {
        RegionChartDataDTO dto = new RegionChartDataDTO();
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        List<Object[]> results;
        if ("range".equals(type)) {
            LocalDateTime start = LocalDateTime.of(currentYear, 1, 1, 0, 0);
            LocalDateTime end = now;
            results = applicationRepository.countByLocationAndAppliedAtBetween(start, end);
        } else if ("month".equals(type) && month != null) {
            LocalDateTime start = LocalDateTime.of(currentYear, month, 1, 0, 0);
            LocalDateTime end = start.plusMonths(1);
            results = applicationRepository.countByLocationAndAppliedAtBetween(start, end);
        } else {
            throw new BadRequestException("Invalid type or month");
        }
        dto.setLabels(results.stream().map(r -> (String) r[0]).collect(Collectors.toList()));
        dto.setCounts(results.stream().map(r -> ((Number) r[1]).longValue()).collect(Collectors.toList()));
        return dto;
    }

    @Override
    public CompanyJobStatsDTO getCompanyJobStats(String type, Integer month) {
        CompanyJobStatsDTO dto = new CompanyJobStatsDTO();
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        List<Object[]> results;
        if ("range".equals(type)) {
            LocalDateTime start = LocalDateTime.of(currentYear, 1, 1, 0, 0);
            LocalDateTime end = now;
            results = jobRepository.countJobsByCompanyAndCreatedAtBetween(start, end);
        } else if ("month".equals(type) && month != null) {
            LocalDateTime start = LocalDateTime.of(currentYear, month, 1, 0, 0);
            LocalDateTime end = start.plusMonths(1);
            results = jobRepository.countJobsByCompanyAndCreatedAtBetween(start, end);
        } else {
            throw new BadRequestException("Invalid type or month");
        }
        dto.setLabels(results.stream().map(r -> (String) r[0]).collect(Collectors.toList()));
        dto.setCounts(results.stream().map(r -> ((Number) r[1]).longValue()).collect(Collectors.toList()));
        return dto;
    }

    private void saveStatusHistory(Application application, ApplicationStatus status) {
        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(application)
                .applicationStatus(status)
                .changedAt(LocalDateTime.now())
                .build();
        applicationStatusHistoryRepository.save(history);
    }

}
