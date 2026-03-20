package com.JobsNow.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserDTO {
    private Integer userId;
    private String email;
    private String fullName;
    private String phone;
    private String roleName;
    /** ACTIVE | DISABLED */
    private String status;
    private Boolean isVerified;
    private String createdAt;
}
