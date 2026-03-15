package com.JobsNow.backend.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobMatchScoreDTO {
    private Long id;
    private Integer profileId;
    private String profileName;
    private String profileTitle;
    private Integer jobId;
    private String jobTitle;
    private String companyName;
    private Integer overallScore;
    private Integer skillMatchScore;
    private Integer aiSemanticScore;
    private String calculatedAt;
}
