package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.linkedin.LinkedInOAuthResult;
import com.JobsNow.backend.dto.linkedin.LinkedInUserInfo;
import com.JobsNow.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class LinkedInTokenVerifier {

    @Value("${linkedin.client-id}")
    private String clientId;

    @Value("${linkedin.client-secret}")
    private String clientSecret;

    @Value("${linkedin.redirect-uri}")
    private String configuredRedirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Exchanges authorization code for tokens, fetches OpenID userinfo.
     */
    public LinkedInOAuthResult completeLinkedInLogin(String code, String redirectUri) {
        String effectiveRedirectUri = resolveRedirectUri(redirectUri);
        Map<String, Object> tokenResponse = exchangeCodeForAccessToken(code, effectiveRedirectUri);
        String accessToken = extractAccessToken(tokenResponse);
        LinkedInOAuthResult.LinkedInOAuthResultBuilder builder = LinkedInOAuthResult.builder()
                .accessToken(accessToken)
                .refreshToken(extractString(tokenResponse, "refresh_token"))
                .expiresInSeconds(extractExpiresIn(tokenResponse))
                .scope(extractString(tokenResponse, "scope"))
                .tokenType(extractString(tokenResponse, "token_type"));

        LinkedInUserInfo userInfo = fetchUserInfo(accessToken);
        builder.userInfo(userInfo);
        return builder.build();
    }

    /**
     * Refreshes LinkedIn access token using refresh token.
     */
    public LinkedInOAuthResult refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("LinkedIn refresh token is missing");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<String, Object> tokenResponse;
        try {
            tokenResponse = restTemplate.postForObject(
                    "https://www.linkedin.com/oauth/v2/accessToken",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
        } catch (Exception e) {
            throw new BadRequestException("Failed to refresh LinkedIn access token");
        }

        String accessToken = extractAccessToken(tokenResponse);
        return LinkedInOAuthResult.builder()
                .accessToken(accessToken)
                .refreshToken(extractString(tokenResponse, "refresh_token"))
                .expiresInSeconds(extractExpiresIn(tokenResponse))
                .scope(extractString(tokenResponse, "scope"))
                .tokenType(extractString(tokenResponse, "token_type"))
                .build();
    }

    private String resolveRedirectUri(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return configuredRedirectUri;
        }
        if (!redirectUri.equals(configuredRedirectUri)) {
            throw new BadRequestException("Invalid redirect URI");
        }
        return redirectUri;
    }

    private Map<String, Object> exchangeCodeForAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            Map<String, Object> tokenResponse = restTemplate.postForObject(
                    "https://www.linkedin.com/oauth/v2/accessToken",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            return tokenResponse;
        } catch (Exception e) {
            throw new BadRequestException("Failed to exchange LinkedIn authorization code");
        }
    }

    private String extractAccessToken(Map<String, Object> response) {
        if (response == null) {
            throw new BadRequestException("LinkedIn token response is empty");
        }
        String accessToken = response.get("access_token") != null
                ? response.get("access_token").toString()
                : null;
        if (accessToken == null || accessToken.isBlank()) {
            throw new BadRequestException("LinkedIn access token not found");
        }
        return accessToken;
    }

    private static String extractString(Map<String, Object> response, String key) {
        if (response == null || response.get(key) == null) {
            return null;
        }
        String v = response.get(key).toString();
        return v.isBlank() ? null : v;
    }

    private static Integer extractExpiresIn(Map<String, Object> response) {
        if (response == null || response.get("expires_in") == null) {
            return null;
        }
        Object raw = response.get("expires_in");
        if (raw instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LinkedInUserInfo fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        Map<String, Object> claims;
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.linkedin.com/v2/userinfo",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            claims = response.getBody();
        } catch (Exception e) {
            throw new BadRequestException("Failed to fetch LinkedIn user info");
        }

        if (claims == null) {
            throw new BadRequestException("LinkedIn user info is empty");
        }

        String email = claims.get("email") != null ? claims.get("email").toString() : null;
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email not provided by LinkedIn");
        }

        String name = claims.get("name") != null ? claims.get("name").toString() : null;
        if (name == null || name.isBlank()) {
            String givenName = claims.get("given_name") != null ? claims.get("given_name").toString() : "";
            String familyName = claims.get("family_name") != null ? claims.get("family_name").toString() : "";
            String fullName = (givenName + " " + familyName).trim();
            name = fullName.isEmpty() ? email : fullName;
        }

        LinkedInUserInfo info = new LinkedInUserInfo();
        info.setEmail(email);
        info.setName(name);
        info.setPicture(claims.get("picture") != null ? claims.get("picture").toString() : null);
        info.setLinkedInId(claims.get("sub") != null ? claims.get("sub").toString() : null);
        return info;
    }
}
