package com.JobsNow.backend.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectJobRequest {
    private Integer jobId;
    private String reason;
}
