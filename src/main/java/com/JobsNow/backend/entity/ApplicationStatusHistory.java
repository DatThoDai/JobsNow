package com.JobsNow.backend.entity;

import com.JobsNow.backend.entity.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer historyId;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private Application application;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus applicationStatus;

    private LocalDateTime changedAt;
}
