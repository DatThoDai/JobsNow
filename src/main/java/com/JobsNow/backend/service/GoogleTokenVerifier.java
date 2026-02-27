package com.JobsNow.backend.service;

import com.JobsNow.backend.exception.BadRequestException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleTokenVerifier {

    @Value("${google.client-id}")
    private String clientId;

    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleUserInfo verify(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        Map<String, Object> claims;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            claims = response;
        } catch (Exception e) {
            throw new BadRequestException("Invalid Google token");
        }

        if (claims == null || !clientId.equals(claims.get("aud"))) {
            throw new BadRequestException("Invalid Google client");
        }

        String email = claims.get("email") != null ? claims.get("email").toString() : null;
        String name = claims.get("name") != null ? claims.get("name").toString() : null;
        String picture = claims.get("picture") != null ? claims.get("picture").toString() : null;
        String sub = claims.get("sub") != null ? claims.get("sub").toString() : null;

        if (email == null || email.isEmpty()) {
            throw new BadRequestException("Email not provided by Google");
        }

        GoogleUserInfo info = new GoogleUserInfo();
        info.setEmail(email);
        info.setName(name);
        info.setPicture(picture);
        info.setGoogleId(sub);
        return info;
    }

    @Getter
    @Setter
    public static class GoogleUserInfo {
        private String email;
        private String name;
        private String picture;
        private String googleId;
    }
}
