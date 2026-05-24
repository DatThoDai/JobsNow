package com.JobsNow.backend.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedCVData {
    private String avatarUrl;
    private String fullName;
    private String title;
    private String email;
    private String phone;
    private String address;
    private String headline;
    private String summary;

    @Builder.Default
    @JsonProperty("work_experiences")
    private List<ParsedWorkExperience> workExperiences = new ArrayList<>();

    @Builder.Default
    private List<ParsedEducation> educations = new ArrayList<>();

    @Builder.Default
    private List<ParsedSkill> skills = new ArrayList<>();

    @Builder.Default
    private List<ParsedProject> projects = new ArrayList<>();

    @Builder.Default
    private List<ParsedLanguage> languages = new ArrayList<>();

    @Builder.Default
    private List<ParsedCertificate> certificates = new ArrayList<>();

    private String suggestedTemplateKey;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedWorkExperience {
        private String company;
        private String position;
        private String duration;
        @JsonProperty("start_date")
        private String startDate;
        @JsonProperty("end_date")
        private String endDate;
        @JsonProperty("is_current")
        private Boolean isCurrent;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedEducation {
        private String school;
        private String major;
        private String degree;
        private String duration;
        @JsonProperty("start_date")
        private String startDate;
        @JsonProperty("end_date")
        private String endDate;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedSkill {
        private String name;
        private String level;
        private String topic;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedProject {
        private String name;
        private String description;
        private String duration;
        private List<String> technologies;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedLanguage {
        private String name;
        private String proficiency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedCertificate {
        private String name;
        private String issuer;
        @JsonProperty("issue_date")
        private String issueDate;
    }
}
