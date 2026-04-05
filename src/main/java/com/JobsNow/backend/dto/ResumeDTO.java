package com.JobsNow.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResumeDTO {
    private Integer resumeId;
    private String resumeName;
    private String resumeUrl;
    private String summary;
    private String templateKey;
    private LocalDateTime uploadedAt;
    private Boolean isPrimary;
}
