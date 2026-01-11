package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.*;
import com.JobsNow.backend.entity.enums.ApplicationStatus;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.ApplicationMapper;
import com.JobsNow.backend.repositories.*;
import com.JobsNow.backend.request.ApplicationRequest;
import com.JobsNow.backend.response.ApplicationDetailResponse;
import com.JobsNow.backend.response.ApplicationOfJobResponse;
import com.JobsNow.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    @Override
    @Transactional
    public void applyForJob(ApplicationRequest request) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));
        if(!job.getIsActive()||job.getIsDeleted()||!job.getIsApproved()){
            throw new BadRequestException("Job is not available for application");
        }
        JobSeekerProfile jobSeekerProfile = jobSeekerProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new RuntimeException("Job seeker profile not found"));
        Resume resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new RuntimeException("Resume not found"));
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
    public void updateApplicationStatus(Integer applicationId, String status) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        try {
            ApplicationStatus newStatus = ApplicationStatus.valueOf(status.toUpperCase());
            application.setApplicationStatus(newStatus);
            applicationRepository.save(application);
            saveStatusHistory(application, newStatus);
        }catch (IllegalArgumentException e){
            throw new BadRequestException("Invalid application status");
        }
    }

    private void saveStatusHistory(Application application, ApplicationStatus status) {
        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(application)
                .applicationStatus(ApplicationStatus.PENDING)
                .changedAt(LocalDateTime.now())
                .build();
        applicationStatusHistoryRepository.save(history);
    }

}
