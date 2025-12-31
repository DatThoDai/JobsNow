package com.JobNexus.backend.controllers;

import com.JobNexus.backend.request.LoginRequest;
import com.JobNexus.backend.request.RegisterRequest;
import com.JobNexus.backend.request.ResendOtpRequest;
import com.JobNexus.backend.request.VerifyOtpRequest;
import com.JobNexus.backend.response.AuthResponse;
import com.JobNexus.backend.response.BaseResponse;
import com.JobNexus.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @ModelAttribute RegisterRequest registerRequest) {
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage(authService.register(registerRequest));
        response.setData(null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("Login successful");
        response.setData(authResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest) {
        authService.verifyOtp(verifyOtpRequest);
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("OTP verified successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequest request) {
        authService.resendOtp(request.getEmail());
        BaseResponse response = new BaseResponse();
        response.setCode(200);
        response.setMessage("OTP resent successfully");
        return ResponseEntity.ok(response);
    }

}
