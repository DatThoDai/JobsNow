package com.JobsNow.backend.service;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.client.RestTemplate;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.entity.Resume;
import com.JobsNow.backend.repositories.ResumeRepository;
import com.JobsNow.backend.request.ImproveCVRequest;
import com.JobsNow.backend.response.ImproveCVResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AICVService {
    private final OpenAIService openAIService;
    private final CVParserService cvParserService;
    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Cải thiện CV từ text hoặc resumeId
     */
    public ImproveCVResponse improveCVFromRequest(ImproveCVRequest request) {
        String cvText;

        if (request.getResumeId() != null) {
            Resume resume = resumeRepository.findById(request.getResumeId())
                    .orElseThrow(() -> new NotFoundException("Resume not found"));
            if (resume.getExtractedText() != null && !resume.getExtractedText().isBlank()) {
                cvText = resume.getExtractedText();
            } else if (resume.getResumeUrl() != null && !resume.getResumeUrl().isBlank()) {
                cvText = downloadAndExtract(resume.getResumeUrl());
                resume.setExtractedText(cvText);
                resumeRepository.save(resume);
            } else {
                throw new BadRequestException("Resume has no content and no file URL");
            }
        } else if (request.getCvText() != null && !request.getCvText().isBlank()) {
            cvText = request.getCvText();
        } else {
            throw new BadRequestException("Either cvText or resumeId is required");
        }
        return analyzeCV(cvText);
    }

    /**
     * Cải thiện CV từ file upload (PDF/DOCX)
     */
    public ImproveCVResponse improveCVFromFile(MultipartFile file) {
        String cvText = cvParserService.extractText(file);
        return analyzeCV(cvText);
    }

    /**
     * Core logic: gửi CV text cho AI phân tích
     */
    private ImproveCVResponse analyzeCV(String cvText) {
        String systemPrompt = """
        You are an expert ATS (Applicant Tracking System) specialist and professional CV reviewer.
        Analyze the provided CV thoroughly and return ONLY valid JSON (no markdown, no code blocks).
        Evaluate CV based on ATS standards.
        Be specific - mention exact phrases that need improvement.
        Do not hallucinate issues if the section already exists and is well-written.
        Score fairly: 0-40 = Poor, 41-60 = Average, 61-80 = Good, 81-100 = Excellent.
        IMPORTANT: If the CV is written in Vietnamese, respond entirely in Vietnamese.
        If the CV is in English, respond in English.
        """;


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
          "improvedSummary": "<rewritten professional summary>",
          "actionItems": ["<priority action 1>", "<priority action 2>"]
        }
        """.formatted(cvText);


        String aiResponse = openAIService.chatCompletion(systemPrompt, userPrompt);

        // Parse JSON response
        try {
            // Loại bỏ markdown code blocks nếu AI vẫn trả về
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

    private String downloadAndExtract(String fileUrl) {
        try {
            // Download file từ URL (S3)
            RestTemplate restTemplate = new RestTemplate();
            byte[] fileBytes = restTemplate.getForObject(fileUrl, byte[].class);

            if (fileBytes == null || fileBytes.length == 0) {
                throw new BadRequestException("Cannot download file from S3");
            }

            // Detect loại file từ URL
            String extension = fileUrl.substring(fileUrl.lastIndexOf(".") + 1)
                    .split("\\?")[0]  // Bỏ query params nếu có (presigned URL)
                    .toLowerCase();

            // Convert byte[] → MultipartFile giả để dùng CVParserService
            // Hoặc parse trực tiếp:
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

}
