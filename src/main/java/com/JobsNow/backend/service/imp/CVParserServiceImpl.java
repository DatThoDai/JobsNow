package com.JobsNow.backend.service.imp;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.service.CVParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
@Slf4j
@Service
public class CVParserServiceImpl implements CVParserService {
    /**
     * Extract text từ file PDF hoặc DOCX
     * @param file MultipartFile upload từ user
     * @return String text content của CV
     */
    @Override
    public String extractText(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new BadRequestException("File name is required");
        }
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> extractFromPDF(file);
            case "docx" -> extractFromDOCX(file);
            default -> throw new BadRequestException(
                    "Unsupported file format: " + extension + ". Only PDF and DOCX are supported.");
        };
    }
    private String extractFromPDF(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("Extracted {} characters from PDF: {}", text.length(), file.getOriginalFilename());
            return normalizeText(text);
        } catch (Exception e) {
            log.error("Failed to parse PDF: {}", e.getMessage());
            throw new BadRequestException("Cannot read PDF file: " + e.getMessage());
        }
    }
    private String extractFromDOCX(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            log.info("Extracted {} characters from DOCX: {}", text.length(), file.getOriginalFilename());
            return normalizeText(text);
        } catch (Exception e) {
            log.error("Failed to parse DOCX: {}", e.getMessage());
            throw new BadRequestException("Cannot read DOCX file: " + e.getMessage());
        }
    }

    private String normalizeText(String text) {
        return text
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("[ \\t]{2,}", " ")
                .trim();
    }
}