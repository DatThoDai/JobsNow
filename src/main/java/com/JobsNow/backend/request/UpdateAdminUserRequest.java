package com.JobsNow.backend.request;

import lombok.Data;

@Data
public class UpdateAdminUserRequest {
    /** ROLE_JOBSEEKER | ROLE_COMPANY | ROLE_ADMIN */
    private String roleName;
    /** ACTIVE | DISABLED */
    private String status;
}
