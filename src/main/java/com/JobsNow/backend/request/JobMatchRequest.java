package com.JobsNow.backend.request;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class JobMatchRequest {
    private Integer profileId;
    private Integer resumeId;
    private Integer jobId;
}
