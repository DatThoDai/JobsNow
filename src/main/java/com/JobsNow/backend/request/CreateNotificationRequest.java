package com.JobsNow.backend.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {
    private String content;
    /** Optional; when null, notification is not tied to an application (e.g. company post rejection). */
    private Integer applicationId;
    private Integer userId;
    /** e.g. SYSTEM, CHAT, COMPANY_POST */
    private String type;
}
