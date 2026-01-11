package com.JobsNow.backend.dto;

import com.JobsNow.backend.entity.enums.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusDTO {
    private ApplicationStatus status;
    private LocalDateTime time;
}
