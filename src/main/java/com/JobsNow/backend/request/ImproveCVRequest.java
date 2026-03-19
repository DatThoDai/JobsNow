package com.JobsNow.backend.request;

import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImproveCVRequest {
    private String cvText;
    private Integer resumeId;
    private String language;
}
