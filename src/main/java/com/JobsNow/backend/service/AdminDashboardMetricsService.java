package com.JobsNow.backend.service;

import com.JobsNow.backend.response.AdminDashboardMetricsResponse;

public interface AdminDashboardMetricsService {
    AdminDashboardMetricsResponse getMetrics(
            String preset,
            String from,
            String to,
            String timezone,
            boolean comparePrevious
    );
}
