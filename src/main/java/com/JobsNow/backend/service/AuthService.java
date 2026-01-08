package com.JobsNow.backend.service;

import com.JobsNow.backend.request.LoginRequest;
import com.JobsNow.backend.request.RegisterRequest;
import com.JobsNow.backend.request.VerifyOtpRequest;
import com.JobsNow.backend.response.AuthResponse;

public interface AuthService {
    String register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
    void verifyOtp(VerifyOtpRequest request);
    void resendOtp(String email);
}
