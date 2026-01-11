package com.JobsNow.backend.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationRequest {
    @NotNull(message = "Job ID is required")
    private Integer jobId;
    @NotNull(message = "Profile ID is required")
    private Integer profileId;
    @NotNull(message = "Resume ID is required")
    private Integer resumeId;
}
