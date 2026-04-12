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
public class NotificationResponse {
    private Integer notificationId;
    private String jobTitle;
    private String content;
    private Boolean isRead;
    private Integer applicationId;
    private Integer userId;
    
    // For Chat notifications
    private String type; // "SYSTEM", "CHAT", etc.
    private String senderName;
    private Integer conversationId;
    
    private LocalDateTime createdAt;
}
