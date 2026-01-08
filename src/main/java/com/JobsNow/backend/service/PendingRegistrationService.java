package com.JobsNow.backend.service;

import com.JobsNow.backend.request.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingRegistrationService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String PREFIX_REGISTRATION = "pending:registration:";
    private static final String PREFIX_OTP = "pending:otp:";
    private static final String PREFIX_LOGO = "pending:logo:";
    private static final long TTL_MINUTES = 5;

    public void savePendingRegistration(String email, RegisterRequest registrationRequest, String otp, String logoUrl){
        try {
            String requestJson = objectMapper.writeValueAsString(registrationRequest);
            redisTemplate.opsForValue().set(PREFIX_REGISTRATION + email, requestJson, Duration.ofMinutes(TTL_MINUTES));
            redisTemplate.opsForValue().set(PREFIX_OTP + email, otp, Duration.ofMinutes(TTL_MINUTES));
            redisTemplate.opsForValue().set(PREFIX_LOGO + email, logoUrl, Duration.ofMinutes(TTL_MINUTES));
            log.info("Lưu thông tin đăng ký tạm thời cho email: " + email);
        }catch (Exception e){
            log.error("Lỗi khi lưu thông tin đăng ký" + e.getMessage());
        }
    }

    public RegisterRequest getPendingRegistration(String email){
        try {
            String requestJson = redisTemplate.opsForValue().get(PREFIX_REGISTRATION + email);
            if(requestJson != null){
                return objectMapper.readValue(requestJson, RegisterRequest.class);
            }
        }catch (Exception e){
            log.error("Lỗi khi lấy thông tin đăng ký tạm thời: " + e.getMessage());
        }
        return null;
    }

    public String getOtp(String email){
        return redisTemplate.opsForValue().get(PREFIX_OTP + email);
    }

    public void updateOtp(String email, String otp){
        if(hasPendingRegistration(email)){
            redisTemplate.opsForValue().set(PREFIX_OTP + email, otp, Duration.ofMinutes(TTL_MINUTES));
            redisTemplate.expire(PREFIX_REGISTRATION + email, Duration.ofMinutes(TTL_MINUTES));
            redisTemplate.expire(PREFIX_LOGO + email, Duration.ofMinutes(TTL_MINUTES));
            log.info("Cập nhật OTP mới cho email: " + email);
        }
    }

    public boolean hasPendingRegistration(String email){
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX_REGISTRATION + email));
    }

    public String getLogoUrl(String email){
        return redisTemplate.opsForValue().get(PREFIX_LOGO + email);
    }

    public void removePendingRegistration(String email){
        redisTemplate.delete(PREFIX_REGISTRATION + email);
        redisTemplate.delete(PREFIX_OTP + email);
        redisTemplate.delete(PREFIX_LOGO + email);
        log.info("Xóa thông tin đăng ký tạm thời cho email: " + email);
    }

}
