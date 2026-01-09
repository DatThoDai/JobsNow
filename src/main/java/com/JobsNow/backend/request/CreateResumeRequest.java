package com.JobsNow.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CreateResumeRequest {
    @NotBlank(message = "Resume name is required")
    private String resumeName;
    private MultipartFile resume;
    private Boolean isPrimary;
}
