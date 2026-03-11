package com.JobsNow.backend.controllers;

import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getNotificationsByUserId(@PathVariable Integer userId) {
        return ResponseFactory.success(notificationService.getNotificationsByUserId(userId));
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<?> countUnread(@PathVariable Integer userId) {
        return ResponseFactory.success(notificationService.countUnreadNotifications(userId));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Integer notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseFactory.successMessage("Marked as read");
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<?> markAllAsRead(@PathVariable Integer userId) {
        notificationService.markAllAsRead(userId);
        return ResponseFactory.successMessage("All notifications marked as read");
    }
}
