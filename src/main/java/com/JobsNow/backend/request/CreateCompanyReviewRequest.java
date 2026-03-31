package com.JobsNow.backend.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanyReviewRequest {
    private Integer rating;
    private String title;
    private String pros;
    private String cons;
    private Boolean recommend;
}
