package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.SocialCredential;
import com.JobsNow.backend.entity.enums.SocialAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialCredentialRepository extends JpaRepository<SocialCredential, Integer> {

    Optional<SocialCredential> findByProviderAndUser_UserId(SocialAuthProvider provider, Integer userId);

    Optional<SocialCredential> findByProviderAndProviderSubject(SocialAuthProvider provider, String providerSubject);
}
