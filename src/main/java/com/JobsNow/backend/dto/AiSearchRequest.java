package com.JobsNow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiSearchRequest {
    private String query;
    private Integer topK = 5;
    private Integer jobId;
}
