package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.constants.JobsNowConstant;
import com.JobsNow.backend.entity.Application;
import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.Job;
import com.JobsNow.backend.entity.JobSeekerProfile;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.entity.enums.ApplicationStatus;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.repositories.ApplicationRepository;
import com.JobsNow.backend.repositories.ApplicationStatusHistoryRepository;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.repositories.JobSeekerProfileRepository;
import com.JobsNow.backend.repositories.ResumeRepository;
import com.JobsNow.backend.request.UpdateApplicationStatusRequest;
import com.JobsNow.backend.response.NotificationResponse;
import com.JobsNow.backend.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private JobRepository jobRepository;
    @Mock private JobSeekerProfileRepository jobSeekerProfileRepository;
    @Mock private ResumeRepository resumeRepository;
    @Mock private ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    @Mock private NotificationServiceImpl notificationService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private EmailService emailService;

    @InjectMocks
    private ApplicationServiceImpl service;

    @Test
    void updateApplicationStatus_shouldThrowWhenInterviewingWithoutValidHtml() {
        Application app = Application.builder()
                .applicationId(123)
                .applicationStatus(ApplicationStatus.PENDING)
                .build();

        when(applicationRepository.findById(123)).thenReturn(Optional.of(app));

        UpdateApplicationStatusRequest req = UpdateApplicationStatusRequest.builder()
                .status("INTERVIEWING")
                .interviewDetailsHtml("<p>&nbsp;</p>")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.updateApplicationStatus(123, req));

        assertTrue(ex.getMessage().contains("interviewDetailsHtml is required"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void updateApplicationStatus_shouldSetInterviewing_andSendNotificationAndEmail() throws Exception {
        User user = new User();
        user.setUserId(7);
        user.setEmail("candidate@test.com");
        user.setFullName("Candidate A");

        JobSeekerProfile profile = new JobSeekerProfile();
        profile.setUser(user);

        Company company = new Company();
        company.setCompanyName("JobsNow Co");

        Job job = new Job();
        job.setTitle("Java Developer");
        job.setCompany(company);

        Application app = Application.builder()
                .applicationId(123)
                .applicationStatus(ApplicationStatus.PENDING)
                .jobSeekerProfile(profile)
                .job(job)
                .build();

        when(applicationRepository.findById(123)).thenReturn(Optional.of(app));
        when(notificationService.createNotification(any())).thenReturn(NotificationResponse.builder().build());

        UpdateApplicationStatusRequest req = UpdateApplicationStatusRequest.builder()
                .status("INTERVIEWING")
                .interviewDetailsHtml("<p>09:00 15/04/2026 - Interview online</p>")
                .build();

        service.updateApplicationStatus(123, req);

        assertEquals(ApplicationStatus.INTERVIEWING, app.getApplicationStatus());
        assertNotNull(app.getInterviewDetailsHtml());

        verify(applicationRepository).save(app);
        verify(applicationStatusHistoryRepository).save(any());
        verify(notificationService).createNotification(any());
        verify(messagingTemplate).convertAndSend(
                eq(JobsNowConstant.WS_TOPIC_NOTIFICATION + 7),
                any(NotificationResponse.class)
        );
        verify(emailService).sendApplicationInterviewEmail(
                eq("candidate@test.com"),
                eq("Candidate A"),
                eq("Java Developer"),
                eq("JobsNow Co"),
                contains("Interview online")
        );
    }
}
