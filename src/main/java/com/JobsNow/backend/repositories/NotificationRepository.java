package com.JobsNow.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.JobsNow.backend.entity.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserUserIdOrderByCreatedAtDesc(Integer userId);
    Long countByUserUserIdAndIsReadFalse(Integer userId);
}
