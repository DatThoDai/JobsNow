package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Application;
import com.JobsNow.backend.entity.Notification;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.mapper.NotificationMapper;
import com.JobsNow.backend.repositories.ApplicationRepository;
import com.JobsNow.backend.repositories.NotificationRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.request.CreateNotificationRequest;
import com.JobsNow.backend.response.NotificationResponse;
import com.JobsNow.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.JobsNow.backend.service.ConversationService;
import org.springframework.context.annotation.Lazy;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    @Lazy
    private final ConversationService conversationService;

    @Override
    public List<NotificationResponse> getNotificationsByUserId(Integer userId) {
        return notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationMapper::toNotificationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new NotFoundException("Application not found"));
        Notification notification = Notification.builder()
                .user(user)
                .application(application)
                .content(request.getContent())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        return NotificationMapper.toNotificationResponse(notification);
    }

    @Override
    public Long countUnreadNotifications(Integer userId) {
        return notificationRepository.countByUserUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(Integer notificationId) {
        Notification noti = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        noti.setIsRead(true);
        notificationRepository.save(noti);

        if ("CHAT".equals(noti.getType()) && noti.getConversationId() != null) {
            conversationService.markMessagesAsRead(noti.getConversationId(), noti.getUser().getUserId());
        }
    }

    @Override
    public void markAllAsRead(Integer userId) {
        List<Notification> notifications = notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> {
            n.setIsRead(true);
            if ("CHAT".equals(n.getType()) && n.getConversationId() != null) {
                conversationService.markMessagesAsRead(n.getConversationId(), userId);
            }
        });
        notificationRepository.saveAll(notifications);
    }
}
