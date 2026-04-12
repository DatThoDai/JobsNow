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
public class ChatAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer attachmentId;
    @ManyToOne
    @JoinColumn(name = "message_id")
    private Message message;
    private String fileName;
    private String fileType;
    private String filePath;
    private LocalDateTime uploadTime;
}
