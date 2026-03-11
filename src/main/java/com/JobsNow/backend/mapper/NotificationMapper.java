package com.JobsNow.backend.mapper;

import com.JobsNow.backend.entity.Notification;
import com.JobsNow.backend.response.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public static NotificationResponse toNotificationResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .jobTitle(n.getApplication().getJob().getTitle())
                .content(n.getContent())
                .isRead(n.getIsRead())
                .applicationId(n.getApplication().getApplicationId())
                .userId(n.getUser().getUserId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
