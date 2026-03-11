package com.JobsNow.backend.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateDTO {
    private Integer id;
    private String title;
    private LocalDate issueDate;
    private String description;
    private Integer sortOrder;
}
