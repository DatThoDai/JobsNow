package com.JobsNow.backend.response;
import lombok.*;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ImproveCVResponse {
    private Integer overallScore;           // Điểm tổng 0-100
    private String overviewFeedback;        // Nhận xét tổng quan
    private List<SectionFeedback> sections; // Phân tích từng phần
    private List<String> missingKeywords;   // Keywords thiếu cho ATS
    private String improvedSummary;         // Gợi ý summary mới
    private List<String> actionItems;       // Việc cần làm cụ thể
    private List<String> extractedSkills;
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SectionFeedback {
        private String section;             // VD: "Experience", "Education"
        private Integer score;              // 0-100
        private List<String> issues;        // Vấn đề tìm thấy
        private List<String> suggestions;   // Gợi ý cải thiện
    }
}
