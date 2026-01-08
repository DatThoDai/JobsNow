package com.JobsNow.backend.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String token;
    private Integer userId;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private String avatar;

    private Integer profileId;

    private Integer companyId;
    private String companyName;
}
