package com.JobsNow.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendCustomEmailRequest {
    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Email content is required")
    private String bodyHtml;
}
