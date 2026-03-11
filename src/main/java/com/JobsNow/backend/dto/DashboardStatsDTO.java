package com.JobsNow.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDTO {
    private long totalApplications;
    private long openJobs;
    private long activeUsers;
    private long pendingApprovals;
}
