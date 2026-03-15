package com.JobsNow.backend.request;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class GenerateCVRequest {
    // cách 1: Lấy dữ liệu từ profile có sẵn
    private Integer profileId;

    // CÁCH 2: User nhập thủ công
    private String fullName;
    private String title;
    private String targetJob;
    private String industry;
    private List<String> skills;
    private List<WorkExpInput> experiences;
    private List<EduInput> educations;
    private List<String> certifications;
    private List<ProjectInput> projects;
    private String language;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class WorkExpInput {
        private String company;
        private String title;
        private String duration;
        private List<String> bullets;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class EduInput {
        private String school;
        private String degree;
        private String major;
        private String duration;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProjectInput {
        private String name;
        private String description;
        private String duration;
    }
}
