package com.JobsNow.backend.mapper;

import com.JobsNow.backend.entity.Notification;
import com.JobsNow.backend.response.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public static NotificationResponse toNotificationResponse(Notification n) {
        String jobTitle = (n.getApplication() != null && n.getApplication().getJob() != null)
                ? n.getApplication().getJob().getTitle() : null;
        Integer appId = n.getApplication() != null ? n.getApplication().getApplicationId() : null;

        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .jobTitle(jobTitle)
                .content(n.getContent())
                .isRead(n.getIsRead())
                .applicationId(appId)
                .userId(n.getUser() != null ? n.getUser().getUserId() : null)
                .type(n.getType() != null ? n.getType() : "SYSTEM")
                .senderName(n.getSenderName())
                .conversationId(n.getConversationId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
