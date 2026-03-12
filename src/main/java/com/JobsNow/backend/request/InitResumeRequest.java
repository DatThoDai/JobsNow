package com.JobsNow.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitResumeRequest {
    @NotBlank(message = "Resume name is required")
    private String resumeName;
}
