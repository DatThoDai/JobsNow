package com.JobsNow.backend.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkedinLoginRequest {

    @NotBlank(message = "Authorization code is required")
    private String code;

    @NotBlank(message = "Role name is required")
    private String roleName;

    private String redirectUri;
}
