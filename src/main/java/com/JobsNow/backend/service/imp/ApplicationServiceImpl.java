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
import com.JobsNow.backend.request.SendCustomEmailRequest;
import com.JobsNow.backend.request.UpdateApplicationStatusRequest;
import com.JobsNow.backend.request.CreateNotificationRequest;
import com.JobsNow.backend.response.ApplicationDetailResponse;
import com.JobsNow.backend.response.ApplicationOfJobResponse;
import com.JobsNow.backend.response.NotificationResponse;
import com.JobsNow.backend.service.ApplicationService;
import com.JobsNow.backend.service.EmailService;
import com.JobsNow.backend.service.AwsS3Service;
import com.JobsNow.backend.service.CVParserService;
import com.JobsNow.backend.service.CandidateQuotaService;
import jakarta.mail.*;
import jakarta.mail.Message;
import jakarta.mail.internet.*;
import jakarta.mail.search.*;

import java.io.*;
import java.nio.file.Files;
import java.time.YearMonth;
import java.util.Properties;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    private final NotificationServiceImpl notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AwsS3Service awsS3Service;
    private final CVParserService cvParserService;
    private final CandidateQuotaService candidateQuotaService;
    private final CompanyRepository companyRepository;

    @Value("${jobsnow.mail.imap.username:${spring.mail.username:}}")
    private String imapUsername;

    @Value("${jobsnow.mail.imap.password:${spring.mail.password:}}")
    private String imapPassword;

    @Value("${jobsnow.mail.imap.host:imap.gmail.com}")
    private String imapHost;

    @Value("${jobsnow.mail.imap.port:993}")
    private String imapPort;
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

        int currentApplyCount = job.getApplyCount() != null ? job.getApplyCount() : 0;
        job.setApplyCount(currentApplyCount + 1);
        jobRepository.save(job);

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
    @Transactional
    public void updateApplicationStatus(Integer applicationId, UpdateApplicationStatusRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new BadRequestException("status is required");
        }
        try {
            ApplicationStatus newStatus = ApplicationStatus.valueOf(request.getStatus().trim().toUpperCase());
            if (newStatus == ApplicationStatus.INTERVIEWING) {
                if (isBlankRichText(request.getInterviewDetailsHtml())) {
                    throw new BadRequestException("interviewDetailsHtml is required when status is INTERVIEWING");
                }
                application.setInterviewDetailsHtml(request.getInterviewDetailsHtml().trim());
            } else {
                application.setInterviewDetailsHtml(null);
            }
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

            sendApplicationStatusEmail(application, newStatus);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid application status");
        }
    }

    private static boolean isBlankRichText(String html) {
        if (html == null) {
            return true;
        }
        String stripped = html.replaceAll("(?s)<[^>]*>", " ").replace("&nbsp;", " ").trim();
        return stripped.isEmpty();
    }

    @Override
    public void sendCustomEmail(Integer applicationId, SendCustomEmailRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!application.getJob().getCompany().getUser().getEmail().equals(email)) {
            throw new BadRequestException("You don't have permission to send email to this application");
        }

        String candidateName = application.getJobSeekerProfile().getUser().getFullName();
        String jobTitle = application.getJob().getTitle();
        String companyName = application.getJob().getCompany().getCompanyName();
        String candidateEmail = application.getJobSeekerProfile().getUser().getEmail();

        if (candidateEmail == null || candidateEmail.isEmpty()) {
            throw new BadRequestException("Candidate does not have an email address");
        }

        String body = request.getBodyHtml() != null ? request.getBodyHtml() : "";
        body = body
                .replace("[Tên Ứng Viên]", HtmlUtils.htmlEscape(candidateName != null ? candidateName : "Ứng viên"))
                .replace("[Tên Công Việc]", HtmlUtils.htmlEscape(jobTitle != null ? jobTitle : ""))
                .replace("[Tên Công Ty]", HtmlUtils.htmlEscape(companyName != null ? companyName : ""));

        try {
            emailService.sendCustomEmail(candidateEmail, request.getSubject(), body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
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
            java.time.YearMonth yearMonth = YearMonth.of(currentYear, month);
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

    private void sendApplicationStatusEmail(Application application, ApplicationStatus status) {
        String toEmail = null;
        try {
            toEmail = application.getJobSeekerProfile().getUser().getEmail();
            if (toEmail == null || toEmail.isBlank()) return;
            String candidateName = application.getJobSeekerProfile().getUser().getFullName();
            String jobTitle = application.getJob().getTitle();
            String companyName = application.getJob().getCompany().getCompanyName();
            if (status == ApplicationStatus.HIRED) {
                emailService.sendApplicationApprovedEmail(toEmail, candidateName, jobTitle, companyName);
            } else if (status == ApplicationStatus.REJECTED) {
                emailService.sendApplicationRejectedEmail(toEmail, candidateName, jobTitle, companyName);
            } else if (status == ApplicationStatus.INTERVIEWING) {
                String bodyHtml = application.getInterviewDetailsHtml();
                emailService.sendApplicationInterviewEmail(toEmail, candidateName, jobTitle, companyName, bodyHtml);
            }
        } catch (Exception e) {
            log.error("Failed to send application status email to {}, applicationId={}, status={}", toEmail, application.getApplicationId(), status, e);
        }
    }

    private void saveStatusHistory(Application application, ApplicationStatus status) {
        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(application)
                .applicationStatus(status)
                .changedAt(LocalDateTime.now())
                .build();
        applicationStatusHistoryRepository.save(history);
    }

    @Override
    @Transactional
    public void applyViaEmail(String email, String fullName, Integer jobId, MultipartFile cvFile) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            Role role = roleRepository.findByRoleName("ROLE_JOBSEEKER")
                    .orElseThrow(() -> new BadRequestException("Default jobseeker role not found"));
            user = new User();
            user.setEmail(email);
            user.setFullName(fullName != null ? fullName : email.substring(0, email.indexOf('@')));
            user.setRole(role);
            user.setIsVerified(true);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);

            JobSeekerProfile profile = new JobSeekerProfile();
            profile.setUser(user);
            profile.setAvatarUrl("https://jobsnow-upload.s3.us-east-1.amazonaws.com/avatars/default-avatar_1771699390597.png");
            jobSeekerProfileRepository.save(profile);

            candidateQuotaService.ensureDefaultQuota(user.getUserId(), 0, 3);
        }

        final User finalUser = user;
        JobSeekerProfile finalProfile = jobSeekerProfileRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    JobSeekerProfile profile = new JobSeekerProfile();
                    profile.setUser(finalUser);
                    profile.setAvatarUrl("https://jobsnow-upload.s3.us-east-1.amazonaws.com/avatars/default-avatar_1771699390597.png");
                    jobSeekerProfileRepository.save(profile);
                    candidateQuotaService.ensureDefaultQuota(finalUser.getUserId(), 0, 3);
                    return profile;
                });

        String extractedText = null;
        String s3Url = null;

        if (cvFile != null && !cvFile.isEmpty()) {
            try {
                String originalFileName = cvFile.getOriginalFilename();
                if (originalFileName == null || originalFileName.isBlank()) {
                    originalFileName = "cv_from_email_" + System.currentTimeMillis() + ".pdf";
                }
                String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
                String baseName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
                String s3Key = "resumes/" + baseName + "_" + System.currentTimeMillis() + extension;
                s3Url = awsS3Service.uploadFileToS3(cvFile.getInputStream(), s3Key, cvFile.getContentType());
                extractedText = cvParserService.extractText(cvFile);
            } catch (Exception e) {
                log.warn("Failed to process attached CV: {}", e.getMessage());
            }
        }

        Resume resume = new Resume();
        resume.setJobSeekerProfile(finalProfile);
        resume.setResumeName("CV_" + System.currentTimeMillis());
        resume.setResumeUrl(s3Url);
        resume.setExtractedText(extractedText);
        resume.setTemplateKey("cvhay-industry-safety");
        resume.setUploadedAt(LocalDateTime.now());
        resume.setIsDeleted(false);
        resume.setIsPrimary(false);
        resumeRepository.save(resume);

        Job job = jobRepository.findById(jobId).orElse(null);
        if (job != null && applicationRepository.existsByJob_JobIdAndJobSeekerProfile_ProfileId(jobId, finalProfile.getProfileId())) {
            return;
        }

        ApplicationRequest appRequest = new ApplicationRequest();
        appRequest.setJobId(jobId);
        appRequest.setProfileId(finalProfile.getProfileId());
        appRequest.setResumeId(resume.getResumeId());

        applyForJob(appRequest);
    }

    @Override
    @Transactional
    public List<String> syncApplicationsFromEmail() {
        String loggedInUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedInUser = userRepository.findByEmail(loggedInUserEmail).orElse(null);
        if (loggedInUser == null) {
            throw new BadRequestException("Logged in user not found");
        }
        Company loggedInCompany = companyRepository.findByUser_UserId(loggedInUser.getUserId()).orElse(null);
        if (loggedInCompany == null) {
            throw new BadRequestException("Company not found for logged in user");
        }
        final Integer loggedInCompanyId = loggedInCompany.getCompanyId();

        List<String> syncedCandidates = new ArrayList<>();
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", imapPort);
        props.put("mail.imaps.ssl.enable", "true");

        try {
            Session session = Session.getInstance(props, null);
            Store store = session.getStore("imaps");
            store.connect(imapHost, imapUsername, imapPassword);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            SearchTerm term = new AndTerm(
                new FlagTerm(new Flags(Flags.Flag.SEEN), false),
                new SubjectTerm("Apply:")
            );

            Message[] messages = inbox.search(term);
            for (Message message : messages) {
                try {
                    String subject = message.getSubject();
                    if (subject == null) continue;

                    java.util.regex.Matcher matcher = Pattern.compile("\\[JobId:\\s*(\\d+)\\]", Pattern.CASE_INSENSITIVE).matcher(subject);
                    Integer jobId = 1;
                    if (matcher.find()) {
                        jobId = Integer.parseInt(matcher.group(1));
                    }

                    Job job = jobRepository.findById(jobId).orElse(null);
                    if (job == null || job.getCompany() == null || !job.getCompany().getCompanyId().equals(loggedInCompanyId)) {
                        continue;
                    }

                    jakarta.mail.Address[] froms = message.getFrom();
                    if (froms == null || froms.length == 0) continue;
                    jakarta.mail.internet.InternetAddress fromAddress = (InternetAddress) froms[0];
                    String email = fromAddress.getAddress();
                    String fullName = fromAddress.getPersonal();

                    try {
                        String bodyText = getTextFromMessage(message);
                        if (bodyText != null && !bodyText.isBlank()) {
                            java.util.regex.Matcher bodyMatcher = java.util.regex.Pattern.compile("tôi là\\s+([^(]+)\\(([^)]+)\\)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(bodyText);
                            if (bodyMatcher.find()) {
                                fullName = bodyMatcher.group(1).trim();
                                email = bodyMatcher.group(2).trim();
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract candidate info from body: {}", e.getMessage());
                    }

                    if (fullName == null || fullName.isBlank()) {
                        fullName = email.substring(0, email.indexOf('@'));
                    }

                    MultipartFile attachmentFile = null;
                    if (message.getContent() instanceof jakarta.mail.Multipart) {
                        jakarta.mail.Multipart multipart = (Multipart) message.getContent();
                        for (int i = 0; i < multipart.getCount(); i++) {
                            BodyPart bodyPart = multipart.getBodyPart(i);
                            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                                (bodyPart.getFileName() != null && !bodyPart.getFileName().isBlank())) {
                                String fileName = bodyPart.getFileName();
                                String contentType = bodyPart.getContentType();
                                InputStream is = bodyPart.getInputStream();
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    baos.write(buffer, 0, bytesRead);
                                }
                                attachmentFile = new InMemoryMultipartFile("cvFile", fileName, contentType, baos.toByteArray());
                                break;
                            }
                        }
                    }

                    if (attachmentFile != null) {
                        applyViaEmail(email, fullName, jobId, attachmentFile);
                        syncedCandidates.add(fullName);
                        message.setFlag(jakarta.mail.Flags.Flag.SEEN, true);
                    }
                } catch (Exception e) {
                    log.error("Failed to process sync for message: {}", message.getSubject(), e);
                }
            }

            inbox.close(true);
            store.close();
        } catch (Exception e) {
            throw new BadRequestException("Failed to sync applications from email: " + e.getMessage());
        }
        return syncedCandidates;
    }

    private String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/*")) {
            return message.getContent().toString();
        }
        if (message.isMimeType("multipart/*")) {
            jakarta.mail.Multipart multipart = (jakarta.mail.Multipart) message.getContent();
            return getTextFromMultipart(multipart);
        }
        return "";
    }

    private String getTextFromMultipart(Multipart multipart) throws Exception {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/*")) {
                result.append(bodyPart.getContent().toString());
            } else if (bodyPart.getContent() instanceof Multipart) {
                result.append(getTextFromMultipart((Multipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }

    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content == null || content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }
    }

    @Override
    @Transactional
    public void sendApplyEmail(Integer jobId, String email, String fullName, String subject, String body, MultipartFile cvFile) {
        try {
            String finalSubject = (subject != null && !subject.isBlank()) ? subject : "Apply: " + fullName + " [JobId: " + jobId + "]";
            String finalBody = (body != null && !body.isBlank()) ? body : "Chào nhà tuyển dụng, tôi là " + fullName + " (" + email + "), tôi muốn ứng tuyển vào công việc của quý công ty.";
            if (!finalBody.contains("<p>") && !finalBody.contains("<br/>") && !finalBody.contains("<div>")) {
                finalBody = "<p>" + finalBody.replace("\n", "<br/>") + "</p>";
            }
            String fileName = cvFile.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                fileName = "CV_" + fullName.replaceAll("\\s+", "_") + ".pdf";
            }
            ByteArrayResource resource = new ByteArrayResource(cvFile.getBytes());
            emailService.sendEmailWithAttachment(
                imapUsername,
                finalSubject,
                finalBody,
                fileName,
                resource,
                cvFile.getContentType()
            );
        } catch (Exception e) {
            throw new BadRequestException("Failed to send apply email: " + e.getMessage());
        }
    }
}
