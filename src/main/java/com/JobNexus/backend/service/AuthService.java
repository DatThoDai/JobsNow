package com.JobNexus.backend.service;

import com.JobNexus.backend.request.LoginRequest;
import com.JobNexus.backend.request.RegisterRequest;
import com.JobNexus.backend.request.VerifyOtpRequest;
import com.JobNexus.backend.response.AuthResponse;

public interface AuthService {
    String register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
    void verifyOtp(VerifyOtpRequest request);
    void resendOtp(String email);
}
