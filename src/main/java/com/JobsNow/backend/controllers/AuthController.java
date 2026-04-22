package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.GoogleLoginRequest;
import com.JobsNow.backend.request.LinkedinLoginRequest;
import com.JobsNow.backend.request.LoginRequest;
import com.JobsNow.backend.request.RegisterRequest;
import com.JobsNow.backend.request.ResendOtpRequest;
import com.JobsNow.backend.request.SendOtpRequest;
import com.JobsNow.backend.request.VerifyLoginOtpRequest;
import com.JobsNow.backend.request.VerifyOtpRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @ModelAttribute RegisterRequest registerRequest) {
        return ResponseFactory.successMessage(authService.register(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseFactory.success(authService.login(loginRequest));
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseFactory.success(authService.loginWithGoogle(request.getIdToken(), request.getRoleName()));
    }

    @PostMapping("/linkedin-login")
    public ResponseEntity<?> loginWithLinkedIn(@Valid @RequestBody LinkedinLoginRequest request) {
        return ResponseFactory.success(
                authService.loginWithLinkedIn(request.getCode(), request.getRoleName(), request.getRedirectUri())
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest) {
        authService.verifyOtp(verifyOtpRequest);
        return ResponseFactory.successMessage("OTP verified successfully");
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequest request) {
        authService.resendOtp(request.getEmail());
        return ResponseFactory.successMessage("OTP resent successfully");
    }

    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmailExists(@RequestParam String email) {
        return ResponseFactory.success(authService.checkEmailExists(email));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendLoginOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendLoginOtp(request.getEmail());
        return ResponseFactory.successMessage("OTP sent successfully");
    }

    @PostMapping("/verify-login-otp")
    public ResponseEntity<?> verifyLoginOtp(@Valid @RequestBody VerifyLoginOtpRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        return ResponseFactory.success(authService.verifyLoginOtp(request.getEmail(), request.getOtp(), clientIp));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return ResponseFactory.success(authService.getCurrentUser(email));
    }
}
