package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.Job;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.JobBoostRepository;
import com.JobsNow.backend.repositories.JobCategoryRepository;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.repositories.JobSkillRepository;
import com.JobsNow.backend.repositories.JobViewEventRepository;
import com.JobsNow.backend.repositories.MajorRepository;
import com.JobsNow.backend.repositories.SkillRepository;
import com.JobsNow.backend.request.CreateJobRequest;
import com.JobsNow.backend.service.CompanyQuotaService;
import com.JobsNow.backend.service.EmailService;
import com.JobsNow.backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock private JobRepository jobRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private JobCategoryRepository jobCategoryRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private JobSkillRepository jobSkillRepository;
    @Mock private JobBoostRepository jobBoostRepository;
    @Mock private JobViewEventRepository jobViewEventRepository;
    @Mock private MajorRepository majorRepository;
    @Mock private EmailService emailService;
    @Mock private CompanyQuotaService companyQuotaService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private JobServiceImpl service;

    @Test
    void createJob_shouldSetPendingWhenIsActiveFalse_andConsumeQuota() {
        Company company = new Company();
        company.setCompanyId(10);
        company.setJobPostCount(2);

        CreateJobRequest request = CreateJobRequest.builder()
                .companyId(10)
                .title("Backend Java")
                .description("desc")
                .requirements("req")
                .benefits("benefits")
                .yearsOfExperience("2 years")
                .educationLevel("BACHELOR")
                .location("HCM")
                .deadline(LocalDate.now().plusDays(15))
                .salaryType("RANGE")
                .salaryMin(1000d)
                .salaryMax(2000d)
                .isActive(false)
                .build();

        when(companyRepository.findById(10)).thenReturn(Optional.of(company));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createJob(request);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeastOnce()).save(jobCaptor.capture());
        Job saved = jobCaptor.getAllValues().get(0);

        assertFalse(Boolean.TRUE.equals(saved.getIsActive()));
        assertTrue(Boolean.TRUE.equals(saved.getIsPending()));
        assertFalse(Boolean.TRUE.equals(saved.getIsApproved()));

        verify(companyQuotaService).consumeJobPost(10);
        verify(companyRepository).save(company);
        assertEquals(3, company.getJobPostCount());
    }

    @Test
    void createJob_shouldThrowWhenSalaryRangeInvalid() {
        Company company = new Company();
        company.setCompanyId(10);

        CreateJobRequest request = CreateJobRequest.builder()
                .companyId(10)
                .title("Backend Java")
                .description("desc")
                .requirements("req")
                .benefits("benefits")
                .yearsOfExperience("2 years")
                .educationLevel("BACHELOR")
                .location("HCM")
                .deadline(LocalDate.now().plusDays(15))
                .salaryType("RANGE")
                .salaryMin(3000d)
                .salaryMax(2000d)
                .build();

        when(companyRepository.findById(10)).thenReturn(Optional.of(company));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.createJob(request));
        assertTrue(ex.getMessage().contains("salaryMin must be <= salaryMax"));
    }
}
