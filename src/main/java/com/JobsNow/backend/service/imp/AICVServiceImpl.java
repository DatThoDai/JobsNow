package com.JobsNow.backend.service.imp;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.entity.Resume;
import com.JobsNow.backend.entity.WorkExperience;
import com.JobsNow.backend.entity.Education;
import com.JobsNow.backend.entity.Project;
import com.JobsNow.backend.entity.Certificate;
import com.JobsNow.backend.entity.ResumeSkill;
import com.JobsNow.backend.repositories.ResumeRepository;
import com.JobsNow.backend.repositories.WorkExperienceRepository;
import com.JobsNow.backend.repositories.EducationRepository;
import com.JobsNow.backend.repositories.ProjectRepository;
import com.JobsNow.backend.repositories.CertificateRepository;
import com.JobsNow.backend.repositories.ResumeSkillRepository;
import com.JobsNow.backend.request.ImproveCVRequest;
import com.JobsNow.backend.response.ImproveCVResponse;
import com.JobsNow.backend.service.AICVService;
import com.JobsNow.backend.service.CVParserService;
import com.JobsNow.backend.service.OpenAIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.JobsNow.backend.entity.*;
import com.JobsNow.backend.repositories.JobSeekerProfileRepository;
import com.JobsNow.backend.request.GenerateCVRequest;
import com.JobsNow.backend.response.GenerateCVResponse;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AICVServiceImpl implements AICVService {
    private final OpenAIService openAIService;
    private final CVParserService cvParserService;
    private final ResumeRepository resumeRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final EducationRepository educationRepository;
    private final ProjectRepository projectRepository;
    private final CertificateRepository certificateRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Cải thiện CV từ text hoặc resumeId
     */
    @Override
    public ImproveCVResponse improveCVFromRequest(ImproveCVRequest request) {
        String cvText;

        if (request.getResumeId() != null) {
            Resume resume = resumeRepository.findById(request.getResumeId())
                    .orElseThrow(() -> new NotFoundException("Resume not found"));
            if (resume.getExtractedText() != null && !resume.getExtractedText().isBlank()) {
                cvText = resume.getExtractedText();
            } else if (resume.getResumeUrl() != null && !resume.getResumeUrl().isBlank()) {
                try {
                    cvText = downloadAndExtract(resume.getResumeUrl());
                    resume.setExtractedText(cvText);
                    resumeRepository.save(resume);
                } catch (BadRequestException ex) {
                    // Fallback cho CV tạo thủ công hoặc file không parse được.
                    cvText = buildStructuredResumeText(resume);
                    if (cvText == null || cvText.isBlank()) {
                        throw ex;
                    }
                    resume.setExtractedText(cvText);
                    resumeRepository.save(resume);
                }
            } else {
                cvText = buildStructuredResumeText(resume);
                if (cvText == null || cvText.isBlank()) {
                    throw new BadRequestException("Resume has no content and no file URL");
                }
                resume.setExtractedText(cvText);
                resumeRepository.save(resume);
            }
        } else if (request.getCvText() != null && !request.getCvText().isBlank()) {
            cvText = request.getCvText();
        } else {
            throw new BadRequestException("Either cvText or resumeId is required");
        }
        return analyzeCV(cvText, request.getLanguage());
    }

    /**
     * Cải thiện CV từ file upload (PDF/DOCX)
     */
    @Override
    public ImproveCVResponse improveCVFromFile(MultipartFile file, String language) {
        String cvText = cvParserService.extractText(file);
        return analyzeCV(cvText, language);
    }

    /**
     * Core logic: gửi CV text cho AI phân tích
     */
    private ImproveCVResponse analyzeCV(String cvText, String language) {
        String normalizedLanguage = language == null ? "auto" : language.trim().toLowerCase();

        String languageInstruction;
        if ("vi".equals(normalizedLanguage)) {
            languageInstruction = "Respond entirely in Vietnamese.";
        } else if ("en".equals(normalizedLanguage)) {
            languageInstruction = "Respond entirely in English.";
        } else {
            languageInstruction = "If the CV is written in Vietnamese, respond entirely in Vietnamese. If the CV is in English, respond in English.";
        }

        String systemPrompt = """
        You are an expert ATS (Applicant Tracking System) specialist and professional CV reviewer.
        Analyze the provided CV thoroughly and return ONLY valid JSON (no markdown, no code blocks).
        Evaluate CV based on ATS standards.
        Be specific - mention exact phrases that need improvement.
        Do not hallucinate issues if the section already exists and is well-written.
        Score fairly: 0-40 = Poor, 41-60 = Average, 61-80 = Good, 81-100 = Excellent.
        The field improvedSummary MUST be a rewritten and optimized version, never a verbatim copy.
        Do not copy full sentences from the original summary; paraphrase with clearer ATS-focused wording.
        If the original summary is already good, still provide a stronger rewritten version with better structure and impact.
        IMPORTANT: %s
        """;

        systemPrompt = systemPrompt.formatted(languageInstruction);


        String userPrompt = """
        Analyze this CV and provide detailed improvement suggestions:
        
        ---CV START---
        %s
        ---CV END---
        
        Return ONLY this JSON structure (no other text):
        {
          "overallScore": <0-100>,
          "overviewFeedback": "<2-3 sentences overall assessment>",
          "sections": [
            {
              "section": "<section name>",
              "score": <0-100>,
              "issues": ["<specific issue>"],
              "suggestions": ["<actionable suggestion>"]
            }
          ],
          "missingKeywords": ["<keyword1>", "<keyword2>"],
          "extractedSkills": ["<skill1>", "<skill2>"],
                    "improvedSummary": "<rewritten professional summary, not copied verbatim from CV>",
          "actionItems": ["<priority action 1>", "<priority action 2>"]
        }
        """.formatted(cvText);


        String aiResponse = openAIService.chatCompletion(systemPrompt, userPrompt);

        try {
            String cleanJson = aiResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            return objectMapper.readValue(cleanJson, ImproveCVResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", aiResponse, e);
            // Fallback: trả raw response nếu parse lỗi
            return ImproveCVResponse.builder()
                    .overallScore(0)
                    .overviewFeedback("AI response could not be parsed. Raw: " + aiResponse)
                    .build();
        }
    }

    private String buildStructuredResumeText(Resume resume) {
        StringBuilder sb = new StringBuilder();

        if (resume.getResumeName() != null && !resume.getResumeName().isBlank()) {
            sb.append("Resume Name: ").append(resume.getResumeName().trim()).append("\n");
        }

        if (resume.getJobSeekerProfile() != null) {
            if (resume.getJobSeekerProfile().getUser() != null
                    && resume.getJobSeekerProfile().getUser().getFullName() != null
                    && !resume.getJobSeekerProfile().getUser().getFullName().isBlank()) {
                sb.append("Candidate: ").append(resume.getJobSeekerProfile().getUser().getFullName().trim()).append("\n");
            }
            if (resume.getJobSeekerProfile().getTitle() != null && !resume.getJobSeekerProfile().getTitle().isBlank()) {
                sb.append("Headline: ").append(resume.getJobSeekerProfile().getTitle().trim()).append("\n");
            }
        }

        if (resume.getSummary() != null && !resume.getSummary().isBlank()) {
            sb.append("\nSummary:\n").append(resume.getSummary().trim()).append("\n");
        }

        List<ResumeSkill> skills = resumeSkillRepository.findByResume_ResumeId(resume.getResumeId());
        if (!skills.isEmpty()) {
            sb.append("\nSkills:\n");
            for (ResumeSkill rs : skills) {
                if (rs.getSkill() == null || rs.getSkill().getSkillName() == null || rs.getSkill().getSkillName().isBlank()) {
                    continue;
                }
                sb.append("- ").append(rs.getSkill().getSkillName().trim());
                if (rs.getLevel() != null && !rs.getLevel().isBlank()) {
                    sb.append(" (").append(rs.getLevel().trim()).append(")");
                }
                sb.append("\n");
            }
        }

        List<WorkExperience> experiences = workExperienceRepository.findByResume_ResumeIdOrderBySortOrderAsc(resume.getResumeId());
        if (!experiences.isEmpty()) {
            sb.append("\nWork Experience:\n");
            for (WorkExperience we : experiences) {
                if (we.getTitle() == null || we.getTitle().isBlank()) {
                    continue;
                }
                sb.append("- ").append(we.getTitle().trim());
                String range = formatDateRange(we.getStartDate(), we.getEndDate());
                if (!range.isBlank()) {
                    sb.append(" [").append(range).append("]");
                }
                if (we.getDescription() != null && !we.getDescription().isBlank()) {
                    sb.append(": ").append(we.getDescription().trim());
                }
                sb.append("\n");
            }
        }

        List<Education> educations = educationRepository.findByResume_ResumeIdOrderBySortOrderAsc(resume.getResumeId());
        if (!educations.isEmpty()) {
            sb.append("\nEducation:\n");
            for (Education edu : educations) {
                if (edu.getTitle() == null || edu.getTitle().isBlank()) {
                    continue;
                }
                sb.append("- ").append(edu.getTitle().trim());
                if (edu.getMajor() != null && edu.getMajor().getName() != null && !edu.getMajor().getName().isBlank()) {
                    sb.append(" (Major: ").append(edu.getMajor().getName().trim()).append(")");
                }
                String range = formatDateRange(edu.getStartDate(), edu.getEndDate());
                if (!range.isBlank()) {
                    sb.append(" [").append(range).append("]");
                }
                if (edu.getDescription() != null && !edu.getDescription().isBlank()) {
                    sb.append(": ").append(edu.getDescription().trim());
                }
                sb.append("\n");
            }
        }

        List<Project> projects = projectRepository.findByResume_ResumeIdOrderBySortOrderAsc(resume.getResumeId());
        if (!projects.isEmpty()) {
            sb.append("\nProjects:\n");
            for (Project p : projects) {
                if (p.getTitle() == null || p.getTitle().isBlank()) {
                    continue;
                }
                sb.append("- ").append(p.getTitle().trim());
                String range = formatDateRange(p.getStartDate(), p.getEndDate());
                if (!range.isBlank()) {
                    sb.append(" [").append(range).append("]");
                }
                if (p.getDescription() != null && !p.getDescription().isBlank()) {
                    sb.append(": ").append(p.getDescription().trim());
                }
                sb.append("\n");
            }
        }

        List<Certificate> certificates = certificateRepository.findByResume_ResumeIdOrderBySortOrderAsc(resume.getResumeId());
        if (!certificates.isEmpty()) {
            sb.append("\nCertifications:\n");
            for (Certificate cert : certificates) {
                if (cert.getTitle() == null || cert.getTitle().isBlank()) {
                    continue;
                }
                sb.append("- ").append(cert.getTitle().trim());
                if (cert.getIssueDate() != null) {
                    sb.append(" (").append(cert.getIssueDate()).append(")");
                }
                if (cert.getDescription() != null && !cert.getDescription().isBlank()) {
                    sb.append(": ").append(cert.getDescription().trim());
                }
                sb.append("\n");
            }
        }

        String built = sb.toString().trim();
        return built.isBlank() ? null : built;
    }

    private String formatDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "";
        }
        String start = startDate != null ? startDate.toString() : "";
        String end = endDate != null ? endDate.toString() : "Present";
        if (start.isBlank()) {
            return end;
        }
        return start + " - " + end;
    }

    private String downloadAndExtract(String fileUrl) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            byte[] fileBytes = restTemplate.getForObject(fileUrl, byte[].class);

            if (fileBytes == null || fileBytes.length == 0) {
                throw new BadRequestException("Cannot download file from S3");
            }

            String extension = fileUrl.substring(fileUrl.lastIndexOf(".") + 1)
                    .split("\\?")[0]
                    .toLowerCase();

            if ("pdf".equals(extension)) {
                PDDocument document = Loader.loadPDF(fileBytes);
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                document.close();
                return text.replaceAll("\\n{3,}", "\n\n").trim();
            } else if ("docx".equals(extension)) {
                XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(fileBytes));
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                String text = extractor.getText();
                extractor.close();
                document.close();
                return text.trim();
            } else {
                throw new BadRequestException("Unsupported file format: " + extension);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to download and parse CV: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public GenerateCVResponse generateCV(GenerateCVRequest request) {
        String inputData;

        if (request.getProfileId() != null) {
            inputData = buildInputFromProfile(
                    request.getProfileId(),
                    request.getTargetJob(),
                    request.getIndustry(),
                    request.getAdditionalInfo()
            );
        } else {
            inputData = buildInputFromRequest(request);
        }

        String lang = (request.getLanguage() != null && request.getLanguage().equalsIgnoreCase("vi"))
                ? "Vietnamese" : "English";

        String systemPrompt = """
            You are an expert Executive Resume Writer and Career Coach.
            Your task is to craft a polished, ATS-optimized CV based on the user's provided input.
            Write the entire CV content in %s.
            Return ONLY valid JSON (no markdown, no code blocks, no explanation).
    
            CRITICAL GUIDELINES:
            1. CAREER TRANSITION HANDLING: If the user's base profile (e.g., IT Developer) is completely different from their "Target Job" (e.g., Marketing):
               - DO NOT delete their employment history (Experiences) or Education. We must avoid creating gaps in their resume.
               - INSTEAD, REFRAME the bullet points of unrelated past jobs to focus heavily on "Transferable Skills" (e.g., agile project management, cross-functional collaboration, data analysis, problem-solving, client communication) rather than deep technical jargon.
               - You MAY filter out completely irrelevant hard skills (e.g., specific programming languages) from the "Skills" list to make room for target-relevant skills.
            2. ADDITIONAL INFORMATION PRIORITY: The "Additional Information" field contains the user's most recent updates. You MUST extract skills and objective goals from this text and seamlessly integrate them into the Summary and Skills sections.
            3. ABSOLUTE ZERO HALLUCINATION MODE:
               - The "experiences" array MUST ONLY contain entries from the "Work Experience" section of the input data. If the input has 2 work experiences, return EXACTLY 2.
               - NEVER create, fabricate, or invent a new work experience entry. Not even from projects, additional info, or assumptions.
               - NEVER invent dates, metrics (e.g. "10,000 reach", "2,000 followers"), or company names.
               - Projects MUST stay in the "projects" array. Do NOT promote them to "experiences".
               - If a date is not in the input, leave the duration field as an empty string "".
            4. If a section has no relevant data, return an empty array [] or empty string "".
            """.formatted(lang);


        String userPrompt = """
            Based on the following input data, generate the professional CV JSON:
            
            %s
            
            FORMATTING RULES:
            - Summary: Create an engaging 2-3 sentence paragraph incorporating the "Additional Information" and highlighting how their past background + new skills make them a unique fit for the "Target Job".
            - Skills: Group logically. Ensure missing skills mentioned in "Additional Info" are added to this list! Filter out hardcore technical skills that are completely useless for the Target Job.
            - Experience: ONLY return the EXACT same number of work experience entries as in the input. Reframe bullet points to highlight transferable skills. Do NOT add new entries.
            - Projects: Keep them as projects. Do NOT convert them into fake work experiences.
            - Target Template: Analyze the user's Target Job and Industry, then suggest ONE of the following template keys. (Pick the most suitable one):
              1. 'cvhay-automotive' (For Mechanics, Automotive, Engineering)
              2. 'cvhay-customer-service' (For Customer Service, Support)
              3. 'cvhay-student' (For Interns, Fresher, Students)
              4. 'cvhay-management' (For Managers, Leads, Directors)
              5. 'cvhay-media' (For Marketing, PR, Media, Content)
              6. 'cvhay-it-software' (For IT, Software Developers, Data)
              7. 'cvhay-sales' (For Sales, Business Development)
              8. 'cvhay-industry-safety' (Default fallback for other general industries)
            
            Return ONLY this JSON strictly:
            {
              "suggestedTemplateKey": "<the chosen template key>",
              "summary": "<professional summary 2-3 sentences>",
              "experiences": [
                {
                  "company": "<company>",
                  "title": "<job title>",
                  "duration": "<duration>",
                  "bullets": ["<achievement/responsibility with action verb>"]
                }
              ],
              "educations": [
                {
                  "school": "<school>",
                  "degree": "<degree>",
                  "major": "<major>",
                  "duration": "<duration>"
                }
              ],
              "skillsSection": "<categorized skills formatted as text>",
              "certifications": ["<cert 1>", "<cert 2>"],
              "projects": [
                {
                  "name": "<project name>",
                  "description": "<improved description>",
                  "duration": "<duration>"
                }
              ]
            }
            """.formatted(inputData);

        String aiResponse = openAIService.chatCompletion(systemPrompt, userPrompt);

        try {
            String cleanJson = aiResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();
            return objectMapper.readValue(cleanJson, GenerateCVResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse Generate CV response: {}", aiResponse, e);
            return GenerateCVResponse.builder()
                    .summary("AI response could not be parsed. Raw: " + aiResponse)
                    .build();
        }
    }

    /**
     * Build input text từ profile trong DB
     */
    private String buildInputFromProfile(Integer profileId, String targetJob, String industry, String additionalInfo) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));

        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(profile.getUser().getFullName()).append("\n");
        sb.append("Title: ").append(profile.getTitle() != null ? profile.getTitle() : "N/A").append("\n");

        if (targetJob != null) sb.append("Target Job: ").append(targetJob).append("\n");
        if (industry != null) sb.append("Industry: ").append(industry).append("\n");
        if (additionalInfo != null && !additionalInfo.isBlank()) {
            sb.append("Additional Information:\n").append(additionalInfo.trim()).append("\n");
        }

        // Skills
        if (profile.getJobSeekerSkills() != null && !profile.getJobSeekerSkills().isEmpty()) {
            sb.append("\nSkills:\n");
            for (JobSeekerSkill js : profile.getJobSeekerSkills()) {
                sb.append("- ").append(js.getSkill().getSkillName());
                if (js.getLevel() != null) sb.append(" (").append(js.getLevel()).append(")");
                if (js.getYearsOfExperience() != null) sb.append(" - ").append(js.getYearsOfExperience()).append(" years");
                sb.append("\n");
            }
        }

        // Work Experience
        if (profile.getWorkExperiences() != null && !profile.getWorkExperiences().isEmpty()) {
            sb.append("\nWork Experience:\n");
            for (WorkExperience we : profile.getWorkExperiences()) {
                sb.append("- ").append(we.getTitle());
                if (we.getLevel() != null) sb.append(" (").append(we.getLevel()).append(")");
                sb.append(", ").append(we.getStartDate()).append(" - ");
                sb.append(we.getEndDate() != null ? we.getEndDate() : "Present").append("\n");
                if (we.getDescription() != null) sb.append("  ").append(we.getDescription()).append("\n");
            }
        }

        // Education
        if (profile.getEducations() != null && !profile.getEducations().isEmpty()) {
            sb.append("\nEducation:\n");
            for (Education edu : profile.getEducations()) {
                sb.append("- ").append(edu.getTitle());
                if (edu.getEducationLevel() != null) sb.append(" (").append(edu.getEducationLevel()).append(")");
                if (edu.getMajor() != null) sb.append(", Major: ").append(edu.getMajor().getName());
                sb.append("\n");
            }
        }

        // Projects
        if (profile.getProjects() != null && !profile.getProjects().isEmpty()) {
            sb.append("\nProjects:\n");
            for (Project prj : profile.getProjects()) {
                sb.append("- ").append(prj.getTitle());
                if (prj.getDescription() != null) sb.append(": ").append(prj.getDescription());
                sb.append("\n");
            }
        }

        // Certificates
        if (profile.getCertificates() != null && !profile.getCertificates().isEmpty()) {
            sb.append("\nCertifications:\n");
            for (Certificate cert : profile.getCertificates()) {
                sb.append("- ").append(cert.getTitle()).append("\n");
            }
        }

        if (sb.toString().lines().count() <= 3) {
            throw new BadRequestException("Profile has insufficient data to generate a CV. " + "Please add at least work experience or skills first.");
        }
        return sb.toString();
    }

    /**
     * Build input text từ request thủ công
     */
    private String buildInputFromRequest(GenerateCVRequest req) {
        StringBuilder sb = new StringBuilder();
        if (req.getFullName() != null) sb.append("Name: ").append(req.getFullName()).append("\n");
        if (req.getTitle() != null) sb.append("Title: ").append(req.getTitle()).append("\n");
        if (req.getTargetJob() != null) sb.append("Target Job: ").append(req.getTargetJob()).append("\n");
        if (req.getIndustry() != null) sb.append("Industry: ").append(req.getIndustry()).append("\n");
        if (req.getAdditionalInfo() != null && !req.getAdditionalInfo().isBlank()) {
            sb.append("Additional Information:\n").append(req.getAdditionalInfo().trim()).append("\n");
        }

        if (req.getSkills() != null) {
            sb.append("\nSkills: ").append(String.join(", ", req.getSkills())).append("\n");
        }

        if (req.getExperiences() != null) {
            sb.append("\nWork Experience:\n");
            for (var exp : req.getExperiences()) {
                sb.append("- ").append(exp.getTitle()).append(" at ").append(exp.getCompany());
                sb.append(" (").append(exp.getDuration()).append(")\n");
                if (exp.getBullets() != null) {
                    for (String b : exp.getBullets()) sb.append("  • ").append(b).append("\n");
                }
            }
        }

        if (req.getEducations() != null) {
            sb.append("\nEducation:\n");
            for (var edu : req.getEducations()) {
                sb.append("- ").append(edu.getDegree()).append(" in ").append(edu.getMajor());
                sb.append(" at ").append(edu.getSchool());
                if (edu.getDuration() != null) sb.append(" (").append(edu.getDuration()).append(")");
                sb.append("\n");
            }
        }

        if (req.getProjects() != null) {
            sb.append("\nProjects:\n");
            for (var prj : req.getProjects()) {
                sb.append("- ").append(prj.getName());
                if (prj.getDescription() != null) sb.append(": ").append(prj.getDescription());
                sb.append("\n");
            }
        }

        if (req.getCertifications() != null) {
            sb.append("\nCertifications: ").append(String.join(", ", req.getCertifications())).append("\n");
        }

        return sb.toString();
    }


}
