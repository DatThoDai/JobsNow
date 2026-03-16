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
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer conversationId;

    @ManyToOne
    @JoinColumn(name = "candidate_user_id")
    private User candidateUser;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_user_id", nullable = false)
    private User employerUser;
    private Integer unreadCountCandidate = 0;
    private Integer unreadCountEmployer = 0;
    private Boolean deletedByCandidate = false;
    private Boolean deletedByEmployer = false;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
