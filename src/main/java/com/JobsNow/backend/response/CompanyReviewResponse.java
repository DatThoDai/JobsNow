package com.JobsNow.backend.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyReviewResponse {
    private Integer reviewId;
    private String userName;
    private Integer rating;
    private String title;
    private String pros;
    private String cons;
    private Boolean recommend;
    private String status;
    private LocalDateTime createdAt;
}
