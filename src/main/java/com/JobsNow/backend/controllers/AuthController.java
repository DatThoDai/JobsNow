package com.JobsNow.backend.controllers;

import com.JobsNow.backend.request.LoginRequest;
import com.JobsNow.backend.request.RegisterRequest;
import com.JobsNow.backend.request.ResendOtpRequest;
import com.JobsNow.backend.request.VerifyOtpRequest;
import com.JobsNow.backend.response.AuthResponse;
import com.JobsNow.backend.response.BaseResponse;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.AuthService;
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

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return ResponseFactory.success(authService.getCurrentUser(email));
    }
}
