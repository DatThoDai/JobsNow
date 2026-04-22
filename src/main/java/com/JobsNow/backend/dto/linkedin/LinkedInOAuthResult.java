package com.JobsNow.backend.dto.linkedin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LinkedInOAuthResult {
    private final String accessToken;
    private final String refreshToken;
    private final Integer expiresInSeconds;
    private final String scope;
    private final String tokenType;
    private final LinkedInUserInfo userInfo;
}
