package com.JobsNow.backend.response;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobMatchResponse {
    private Integer overallScore;
    private Integer skillMatchScore;
    private Integer experienceMatchScore;
    private Integer educationMatchScore;
    private Integer ruleBasedScore;
    private Integer aiSemanticScore;
    private String aiFeedback;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> recommendations;
    private String jobTitle;
    private String companyName;
}
