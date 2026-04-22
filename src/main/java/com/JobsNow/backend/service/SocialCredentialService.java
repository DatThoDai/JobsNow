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

    private final SocialCredentialRepository socialCredentialRepository;
    private final TokenEncryptionService tokenEncryption;

    @Transactional
    public void upsertLinkedIn(User user, LinkedInOAuthResult result) {
        if (!tokenEncryption.isEnabled()) {
            throw new BadRequestException(
                    "LinkedIn token storage requires oauth.token.encryption.key (Base64 AES-256 key)."
            );
        }

        LinkedInUserInfo info = result.getUserInfo();
        if (info == null || info.getLinkedInId() == null || info.getLinkedInId().isBlank()) {
            throw new BadRequestException("LinkedIn subject (sub) is missing");
        }

        Instant expiresAt = null;
        if (result.getExpiresInSeconds() != null && result.getExpiresInSeconds() > 0) {
            expiresAt = Instant.now().plusSeconds(result.getExpiresInSeconds());
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
        cred.setAccessTokenEnc(tokenEncryption.encrypt(result.getAccessToken()));

        if (result.getRefreshToken() != null) {
            cred.setRefreshTokenEnc(tokenEncryption.encrypt(result.getRefreshToken()));
        } else if (cred.getId() == null) {
            cred.setRefreshTokenEnc(null);
        }

        cred.setExpiresAt(expiresAt);
        cred.setScope(result.getScope());

        socialCredentialRepository.save(cred);
    }
}
