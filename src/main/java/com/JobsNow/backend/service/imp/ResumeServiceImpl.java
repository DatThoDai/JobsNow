package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.dto.ResumeDTO;
import com.JobsNow.backend.entity.JobSeekerProfile;
import com.JobsNow.backend.entity.Resume;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.ResumeMapper;
import com.JobsNow.backend.repositories.JobSeekerProfileRepository;
import com.JobsNow.backend.repositories.ResumeRepository;
import com.JobsNow.backend.request.CreateResumeRequest;
import com.JobsNow.backend.request.InitResumeRequest;
import com.JobsNow.backend.request.UpdateResumeRequest;
import com.JobsNow.backend.service.AwsS3Service;
import com.JobsNow.backend.service.CVParserService;
import com.JobsNow.backend.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private static final String DEFAULT_TEMPLATE_KEY = "cvhay-industry-safety";
    private static final Set<String> SUPPORTED_TEMPLATE_KEYS = Set.of(
        "cvhay-industry",
        "cvhay-student",
        "cvhay-industry-safety",
        "cvhay-automotive",
        "cvhay-customer-service",
        "cvhay-specialist",
        "cvhay-management",
        "cvhay-media",
        "cvhay-it-software",
        "cvhay-sales"
    );

    private final ResumeRepository resumeRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final AwsS3Service awsS3Service;
    private final CVParserService cvParserService;

    private String resolveTemplateKey(String requestedTemplateKey) {
        if (requestedTemplateKey == null || requestedTemplateKey.isBlank()) {
            return DEFAULT_TEMPLATE_KEY;
        }
        String normalized = requestedTemplateKey.trim().toLowerCase();
        if ("cvhay-industry".equals(normalized)) {
            return "cvhay-industry-safety";
        }
        return SUPPORTED_TEMPLATE_KEYS.contains(normalized) ? normalized : DEFAULT_TEMPLATE_KEY;
    }

    @Override
    public void createResume(Integer profileId, CreateResumeRequest request) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        boolean isExist = resumeRepository.existsByResumeNameAndJobSeekerProfile_ProfileId(request.getResumeName(), profile.getProfileId());
        if (isExist) {
            throw new BadRequestException("Resume with the same name already exists");
        }
        try {
            String resumeFileName = request.getResume().getOriginalFilename();
            InputStream inputStream = request.getResume().getInputStream();
            String contentType = request.getResume().getContentType();
            String extension = resumeFileName.substring(resumeFileName.lastIndexOf("."));
            String baseName = resumeFileName.substring(0, resumeFileName.lastIndexOf("."));
            String s3Key = "resumes/" + baseName + "_" + System.currentTimeMillis() + extension;
            String s3Url = awsS3Service.uploadFileToS3(inputStream, s3Key, contentType);
            String extractedText = null;
            try {
                extractedText = cvParserService.extractText(request.getResume());
            } catch (Exception e) {
                log.warn("Failed to extract text for uploaded resume {}: {}", request.getResumeName(), e.getMessage());
            }

            Resume resume = new Resume();
            resume.setJobSeekerProfile(profile);
            resume.setResumeName(request.getResumeName());
            resume.setResumeUrl(s3Url);
            resume.setExtractedText(extractedText);
            resume.setTemplateKey(resolveTemplateKey(request.getTemplateKey()));
            resume.setUploadedAt(LocalDateTime.now());
            resume.setIsDeleted(false);
            resume.setIsPrimary(false);

            resumeRepository.save(resume);
        } catch (IOException e) {
            throw new BadRequestException("Failed to upload resume: " + e.getMessage());
        }
    }

    @Override
    public ResumeDTO initResume(Integer profileId, InitResumeRequest request) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        boolean isExist = resumeRepository.existsByResumeNameAndJobSeekerProfile_ProfileId(request.getResumeName(), profile.getProfileId());
        if (isExist) {
            throw new BadRequestException("Resume with the same name already exists");
        }
        Resume resume = new Resume();
        resume.setJobSeekerProfile(profile);
        resume.setResumeName(request.getResumeName());
        resume.setResumeUrl(null);
        resume.setTemplateKey(resolveTemplateKey(request.getTemplateKey()));
        resume.setUploadedAt(LocalDateTime.now());
        resume.setIsDeleted(false);
        resume.setIsPrimary(false);
        resumeRepository.save(resume);
        return ResumeMapper.toResumeDTO(resume);
    }

    @Override
    public ResumeDTO updateResume(Integer resumeId, UpdateResumeRequest request) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        if (Boolean.TRUE.equals(resume.getIsDeleted())) {
            throw new BadRequestException("Resume has been deleted");
        }
        if (request.getResumeName() != null && !request.getResumeName().isBlank()) {
            resume.setResumeName(request.getResumeName());
        }
        if (request.getSummary() != null) {
            resume.setSummary(request.getSummary());
        }
        if (request.getTemplateKey() != null) {
            resume.setTemplateKey(resolveTemplateKey(request.getTemplateKey()));
        }
        resumeRepository.save(resume);
        return ResumeMapper.toResumeDTO(resume);
    }

    @Override
    public void deleteResume(Integer resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        if(resume.getIsDeleted()) {
            throw new BadRequestException("Resume has been deleted");
        }
        resume.setIsDeleted(true);
        resumeRepository.save(resume);
    }

    @Override
    public List<ResumeDTO> getResumesByProfileId(Integer profileId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        return resumeRepository
                .findByJobSeekerProfile_ProfileIdAndIsDeletedFalse(profile.getProfileId())
                .stream()
                .map(ResumeMapper::toResumeDTO)
                .toList();
    }

    @Override
    public void setPrimaryResume(Integer resumeId, Integer profileId) {
        List<Resume> resumes = resumeRepository.findByJobSeekerProfile_ProfileIdAndIsDeletedFalse(profileId);
        resumes.forEach(r -> r.setIsPrimary(r.getResumeId().equals(resumeId)));
        resumeRepository.saveAll(resumes);
    }
}
