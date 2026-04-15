package com.JobsNow.backend.response;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GenerateCVResponse {
    private String summary;
    private List<ExperienceSection> experiences;
    private List<EducationSection> educations;
    private String skillsSection;
    private List<String> certifications;
    private List<ProjectSection> projects;
    private String suggestedTemplateKey;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExperienceSection {
        private String company;
        private String title;
        private String duration;
        private List<String> bullets;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EducationSection {
        private String school;
        private String degree;
        private String major;
        private String duration;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProjectSection {
        private String name;
        private String description;
        private String duration;
    }
}
