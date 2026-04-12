package com.JobsNow.backend.response;
import lombok.*;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ImproveCVResponse {
    private Integer overallScore;
    private String overviewFeedback;
    private List<SectionFeedback> sections;
    private List<String> missingKeywords;
    private String improvedSummary;
    private List<String> actionItems;
    private List<String> extractedSkills;
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SectionFeedback {
        private String section;
        private Integer score;
        private List<String> issues;
        private List<String> suggestions;
    }
}
