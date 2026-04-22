package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.linkedin.LinkedInOAuthResult;
import com.JobsNow.backend.dto.linkedin.LinkedInUserInfo;
import com.JobsNow.backend.entity.SocialCredential;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.entity.enums.SocialAuthProvider;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.repositories.SocialCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialCredentialService {
    private static final long REFRESH_SKEW_SECONDS = 120;

    private final SocialCredentialRepository socialCredentialRepository;
    private final LinkedInTokenVerifier linkedInTokenVerifier;

    @Transactional
    public void upsertLinkedIn(User user, LinkedInOAuthResult result) {
        LinkedInUserInfo info = result.getUserInfo();
        if (info == null || info.getLinkedInId() == null || info.getLinkedInId().isBlank()) {
            throw new BadRequestException("LinkedIn subject (sub) is missing");
        }

        Optional<SocialCredential> byUser =
                socialCredentialRepository.findByProviderAndUser_UserId(SocialAuthProvider.LINKEDIN, user.getUserId());
        Optional<SocialCredential> bySubject =
                socialCredentialRepository.findByProviderAndProviderSubject(SocialAuthProvider.LINKEDIN, info.getLinkedInId());

        if (bySubject.isPresent()) {
            Integer existingUserId = bySubject.get().getUser().getUserId();
            if (!existingUserId.equals(user.getUserId())) {
                throw new BadRequestException("This LinkedIn account is already linked to another user.");
            }
        }

        final SocialCredential cred;
        if (byUser.isPresent()) {
            cred = byUser.get();
        } else if (bySubject.isPresent()) {
            cred = bySubject.get();
        } else {
            cred = new SocialCredential();
            cred.setUser(user);
            cred.setProvider(SocialAuthProvider.LINKEDIN);
        }

        cred.setProviderSubject(info.getLinkedInId());
        applyTokenPayload(cred, result);

        socialCredentialRepository.save(cred);
    }

    @Transactional
    public String getValidLinkedInAccessToken(Integer userId) {
        SocialCredential cred = socialCredentialRepository
                .findByProviderAndUser_UserId(SocialAuthProvider.LINKEDIN, userId)
                .orElseThrow(() -> new BadRequestException("LinkedIn account is not connected"));

        if (!needsRefresh(cred)) {
            return cred.getAccessTokenEnc();
        }

        if (cred.getRefreshTokenEnc() == null || cred.getRefreshTokenEnc().isBlank()) {
            throw new BadRequestException("LinkedIn token expired. Please reconnect LinkedIn.");
        }

        String refreshToken = cred.getRefreshTokenEnc();
        LinkedInOAuthResult refreshed = linkedInTokenVerifier.refreshAccessToken(refreshToken);

        applyTokenPayload(cred, refreshed);
        socialCredentialRepository.save(cred);

        return cred.getAccessTokenEnc();
    }

    private boolean needsRefresh(SocialCredential cred) {
        if (cred.getExpiresAt() == null) {
            return false;
        }
        Instant threshold = Instant.now().plusSeconds(REFRESH_SKEW_SECONDS);
        return !cred.getExpiresAt().isAfter(threshold);
    }

    private void applyTokenPayload(SocialCredential cred, LinkedInOAuthResult payload) {
        Instant expiresAt = null;
        if (payload.getExpiresInSeconds() != null && payload.getExpiresInSeconds() > 0) {
            expiresAt = Instant.now().plusSeconds(payload.getExpiresInSeconds());
        }

        cred.setAccessTokenEnc(payload.getAccessToken());

        if (payload.getRefreshToken() != null && !payload.getRefreshToken().isBlank()) {
            cred.setRefreshTokenEnc(payload.getRefreshToken());
        } else if (cred.getId() == null) {
            cred.setRefreshTokenEnc(null);
        }

        cred.setExpiresAt(expiresAt);
        cred.setScope(payload.getScope());
    }
}
