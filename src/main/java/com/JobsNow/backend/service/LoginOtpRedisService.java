package com.JobsNow.backend.service;

import com.JobsNow.backend.dto.OtpLoginData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginOtpRedisService {
    private static final String PREFIX_OTP_LOGIN = "otp:login:";
    private static final String PREFIX_COOLDOWN = "otp:cooldown:";
    private static final String PREFIX_IP_LOCK = "otp:iplock:";
    private static final int TTL_OTP_SECONDS = 300;
    private static final int TTL_COOLDOWN_SECONDS = 60;
    private static final int TTL_IP_LOCK_SECONDS = 300;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveOtp(String email, String hashOtp) {
        try {
            OtpLoginData data = new OtpLoginData(hashOtp, 0);
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(PREFIX_OTP_LOGIN + email, json, Duration.ofSeconds(TTL_OTP_SECONDS));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save OTP data", e);
        }
    }

    public OtpLoginData getOtpData(String email) {
        try {
            String json = redisTemplate.opsForValue().get(PREFIX_OTP_LOGIN + email);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, OtpLoginData.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void updateOtpData(String email, OtpLoginData data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(PREFIX_OTP_LOGIN + email, json, Duration.ofSeconds(TTL_OTP_SECONDS));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update OTP data", e);
        }
    }

    public void deleteOtp(String email) {
        redisTemplate.delete(PREFIX_OTP_LOGIN + email);
    }

    public void setCooldown(String email) {
        redisTemplate.opsForValue().set(PREFIX_COOLDOWN + email, "1", Duration.ofSeconds(TTL_COOLDOWN_SECONDS));
    }

    public boolean hasCooldown(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX_COOLDOWN + email));
    }

    public void setIplock(String ip) {
        redisTemplate.opsForValue().set(PREFIX_IP_LOCK + ip, "1", Duration.ofSeconds(TTL_IP_LOCK_SECONDS));
    }

    public boolean hasIplock(String ip) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX_IP_LOCK + ip));
    }
}
