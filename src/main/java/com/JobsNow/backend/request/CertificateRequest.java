package com.JobsNow.backend.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CertificateRequest {
    private Integer id;
    @NotBlank(message = "Title is required")
    private String title;
    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;
    private String description;
    private Integer sortOrder;
}
