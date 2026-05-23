package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.JobSeekerProfile;
import com.JobsNow.backend.response.CVImportResult;
import com.JobsNow.backend.response.ParsedCVData;
import com.JobsNow.backend.service.CVImportService;
import com.JobsNow.backend.service.CVParserService;
import com.JobsNow.backend.service.OpenAIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CVImportServiceImpl implements CVImportService {

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

    private final CVParserService cvParserService;
    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper snakeCaseMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Override
    public CVImportResult parseFromBytes(byte[] fileBytes, String fileName, JobSeekerProfile profile) {
        if (fileBytes == null || fileBytes.length == 0) {
            return CVImportResult.builder()
                    .parseStatus(CVImportResult.ParseStatus.SKIPPED)
                    .warnings(List.of("No file provided"))
                    .build();
        }
        try {
            String cvText = cvParserService.extractText(fileBytes, fileName);
            return parseFromText(cvText, profile);
        } catch (Exception e) {
            log.warn("CV text extraction failed: {}", e.getMessage());
            return CVImportResult.builder()
                    .parseStatus(CVImportResult.ParseStatus.FAILED)
                    .warnings(List.of("Cannot read file: " + e.getMessage()))
                    .build();
        }
    }

    @Override
    public CVImportResult parseFromFile(MultipartFile file, JobSeekerProfile profile) {
        if (file == null || file.isEmpty()) {
            return CVImportResult.builder()
                    .parseStatus(CVImportResult.ParseStatus.SKIPPED)
                    .warnings(List.of("No file provided"))
                    .build();
        }
        String cvText;
        try {
            cvText = cvParserService.extractText(file);
        } catch (Exception e) {
            log.warn("CV text extraction failed: {}", e.getMessage());
            return CVImportResult.builder()
                    .parseStatus(CVImportResult.ParseStatus.FAILED)
                    .warnings(List.of("Cannot read file: " + e.getMessage()))
                    .build();
        }
        if (cvText == null || cvText.isBlank()) {
            return CVImportResult.builder()
                    .parseStatus(CVImportResult.ParseStatus.FAILED)
                    .warnings(List.of("File contains no extractable text (scanned PDF may need OCR)"))
                    .build();
        }
        return parseFromText(cvText, profile);
    }

    @Override
    public CVImportResult parseFromText(String cvText, JobSeekerProfile profile) {
        if (cvText == null || cvText.isBlank()) {
            return CVImportResult.builder()
                    .parseStatus(CVImportResult.ParseStatus.SKIPPED)
                    .warnings(List.of("Empty CV text"))
                    .build();
        }

        String profileHint = buildProfileHint(profile);
        String systemPrompt = """
                You are an expert CV parser. Extract structured data from the CV text.
                Return ONLY valid JSON (no markdown, no code blocks, no explanation).
                ZERO HALLUCINATION: only include facts explicitly present in the CV text.
                If a section is missing, use an empty array [] or empty string "".
                Dates: use MM/YYYY or YYYY when visible; otherwise leave empty.
                For each work experience, education, and project: put the main label in structured fields
                (company, position, school, name) but put ALL bullet points, responsibilities, achievements,
                and extra details into the "description" field. Do not summarize to one short sentence when
                the CV lists multiple bullets — preserve them with newlines or bullet characters.
                """;

        String userPrompt = """
                %s
                
                Parse this CV and return ONLY this JSON structure:
                {
                  "fullName": "",
                  "title": "",
                  "email": "",
                  "phone": "",
                  "address": "",
                  "headline": "",
                  "summary": "",
                  "work_experiences": [
                    {
                      "company": "",
                      "position": "",
                      "duration": "",
                      "start_date": "",
                      "end_date": "",
                      "is_current": false,
                      "description": ""
                    }
                  ],
                  "educations": [
                    {
                      "school": "",
                      "major": "",
                      "degree": "",
                      "duration": "",
                      "start_date": "",
                      "end_date": "",
                      "description": ""
                    }
                  ],
                  "skills": [{ "name": "", "level": "", "topic": "" }],
                  "projects": [{ "name": "", "description": "", "duration": "", "technologies": [] }],
                  "languages": [{ "name": "", "proficiency": "" }],
                  "certificates": [{ "name": "", "issuer": "", "issue_date": "" }],
                  "suggestedTemplateKey": "cvhay-industry-safety"
                }
                
                For suggestedTemplateKey pick ONE of:
                cvhay-automotive, cvhay-customer-service, cvhay-student, cvhay-management,
                cvhay-media, cvhay-it-software, cvhay-sales, cvhay-industry-safety
                
                ---CV START---
                %s
                ---CV END---
                """.formatted(profileHint, cvText);

        try {
            String aiResponse = openAIService.chatCompletion(systemPrompt, userPrompt);
            String cleanJson = aiResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            ParsedCVData parsed = objectMapper.readValue(cleanJson, ParsedCVData.class);
            enrichFromProfile(parsed, profile);
            normalizeTemplateKey(parsed);

            String json = snakeCaseMapper.writeValueAsString(parsed);
            List<String> warnings = validateParsed(parsed);

            CVImportResult.ParseStatus status = warnings.isEmpty()
                    ? CVImportResult.ParseStatus.SUCCESS
                    : CVImportResult.ParseStatus.PARTIAL;

            return CVImportResult.builder()
                    .parseStatus(status)
                    .parsedCv(parsed)
                    .extractedTextJson(json)
                    .warnings(warnings)
                    .build();
        } catch (Exception e) {
            log.error("CV import parse failed: {}", e.getMessage(), e);
            return CVImportResult.builder()
                    .parseStatus(CVImportResult.ParseStatus.FAILED)
                    .warnings(List.of("AI parse failed: " + e.getMessage()))
                    .build();
        }
    }

    private String buildProfileHint(JobSeekerProfile profile) {
        if (profile == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Known profile hints (use only if CV text lacks them):\n");
        if (profile.getUser() != null && profile.getUser().getFullName() != null) {
            sb.append("- Name: ").append(profile.getUser().getFullName()).append("\n");
        }
        if (profile.getUser() != null && profile.getUser().getEmail() != null) {
            sb.append("- Email: ").append(profile.getUser().getEmail()).append("\n");
        }
        if (profile.getUser() != null && profile.getUser().getPhone() != null) {
            sb.append("- Phone: ").append(profile.getUser().getPhone()).append("\n");
        }
        if (profile.getTitle() != null) {
            sb.append("- Title: ").append(profile.getTitle()).append("\n");
        }
        return sb.toString();
    }

    private void enrichFromProfile(ParsedCVData parsed, JobSeekerProfile profile) {
        if (profile == null || parsed == null) {
            return;
        }
        if (isBlank(parsed.getFullName()) && profile.getUser() != null) {
            parsed.setFullName(profile.getUser().getFullName());
        }
        if (isBlank(parsed.getEmail()) && profile.getUser() != null) {
            parsed.setEmail(profile.getUser().getEmail());
        }
        if (isBlank(parsed.getPhone()) && profile.getUser() != null) {
            parsed.setPhone(profile.getUser().getPhone());
        }
        if (isBlank(parsed.getAddress())) {
            parsed.setAddress(profile.getAddress());
        }
        if (isBlank(parsed.getTitle())) {
            parsed.setTitle(profile.getTitle());
        }
        if (isBlank(parsed.getHeadline())) {
            parsed.setHeadline(profile.getTitle());
        }
    }

    private void normalizeTemplateKey(ParsedCVData parsed) {
        if (parsed == null) {
            return;
        }
        String key = parsed.getSuggestedTemplateKey();
        if (key == null || key.isBlank()) {
            parsed.setSuggestedTemplateKey("cvhay-industry-safety");
            return;
        }
        String normalized = key.trim().toLowerCase();
        if ("cvhay-industry".equals(normalized)) {
            normalized = "cvhay-industry-safety";
        }
        if (!SUPPORTED_TEMPLATE_KEYS.contains(normalized)) {
            parsed.setSuggestedTemplateKey("cvhay-industry-safety");
        } else {
            parsed.setSuggestedTemplateKey(normalized);
        }
    }

    private List<String> validateParsed(ParsedCVData parsed) {
        List<String> warnings = new ArrayList<>();
        if (parsed == null) {
            warnings.add("Parsed data is empty");
            return warnings;
        }
        if (isBlank(parsed.getFullName())) {
            warnings.add("Full name not detected");
        }
        boolean hasContent = (parsed.getWorkExperiences() != null && !parsed.getWorkExperiences().isEmpty())
                || (parsed.getEducations() != null && !parsed.getEducations().isEmpty())
                || (parsed.getSkills() != null && !parsed.getSkills().isEmpty());
        if (!hasContent) {
            warnings.add("No work experience, education, or skills detected");
        }
        return warnings;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
