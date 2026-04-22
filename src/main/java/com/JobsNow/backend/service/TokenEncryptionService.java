package com.JobsNow.backend.service;

import com.JobsNow.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TokenEncryptionService {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int AES_KEY_BYTES = 32;

    private final SecretKey secretKey;
    private final boolean enabled;

    public TokenEncryptionService(@Value("${oauth.token.encryption.key:}") String keyBase64) {
        if (keyBase64 == null || keyBase64.isBlank()) {
            this.secretKey = null;
            this.enabled = false;
            return;
        }
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64.trim());
        if (keyBytes.length != AES_KEY_BYTES) {
            throw new IllegalStateException("oauth.token.encryption.key must decode to exactly 32 bytes (AES-256)");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        if (!enabled) {
            throw new BadRequestException(
                    "OAuth token encryption is not configured. Set oauth.token.encryption.key (Base64, 32 bytes)."
            );
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
            buf.put(iv);
            buf.put(cipherText);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new BadRequestException("Failed to encrypt token");
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        if (!enabled) {
            throw new BadRequestException("OAuth token encryption is not configured.");
        }
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            ByteBuffer buf = ByteBuffer.wrap(all);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buf.get(iv);
            byte[] cipherBytes = new byte[buf.remaining()];
            buf.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BadRequestException("Failed to decrypt token");
        }
    }
}
