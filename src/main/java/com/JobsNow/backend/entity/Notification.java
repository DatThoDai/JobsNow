package com.JobsNow.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;

    @Column(columnDefinition = "TEXT")
    private String content;
    private Boolean isRead;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = true)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String type; // "SYSTEM", "CHAT"
    private String senderName;
    private Integer conversationId;

    private LocalDateTime createdAt;
}
