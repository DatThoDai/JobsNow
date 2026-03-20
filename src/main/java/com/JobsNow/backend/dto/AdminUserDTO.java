package com.JobsNow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {
    private Integer userId;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}
