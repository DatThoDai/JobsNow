package com.JobsNow.backend.service;

import com.JobsNow.backend.request.CreateNotificationRequest;
import com.JobsNow.backend.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getNotificationsByUserId(Integer userId);
    NotificationResponse createNotification(CreateNotificationRequest request);
    Long countUnreadNotifications(Integer userId);
    void markAsRead(Integer notificationId);
    void markAllAsRead(Integer userId);
}
