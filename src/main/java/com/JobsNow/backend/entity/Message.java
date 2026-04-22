package com.JobsNow.backend.entity;

import com.JobsNow.backend.entity.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer messageId;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;
    private Boolean isRead = false;
    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.TEXT;
    private LocalDateTime sentAt;
}
