package com.JobsNow.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSearchResponse {
    private String answer;
    private List<CandidateMatch> candidates;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CandidateMatch {
        private Integer profileId;
        private String fullName;
        private String email;
        private String title;
        private String avatarUrl;
        private List<String> skills;
        private String experience;
        private Double relevanceScore;
        private String matchReason;
    }
}
