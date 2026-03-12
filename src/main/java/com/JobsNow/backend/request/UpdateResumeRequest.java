package com.JobsNow.backend.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateResumeRequest {
    private String resumeName;
    private String summary;
}
