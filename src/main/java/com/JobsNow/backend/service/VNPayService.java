package com.JobsNow.backend.service;

import java.util.Map;

public interface VNPayService {
    String createPaymentUrl(Integer userId, Integer planId, Integer jobId);
    void handlePaymentCallback(Map<String, String> params);
}
