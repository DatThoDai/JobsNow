package com.JobsNow.backend.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardMetricsResponse {
    private RangeInfo range;
    private KpiBlock kpis;
    private List<TrendPoint> trend;
    private List<StatusCountItem> orderStatusDistribution;
    private List<ScopeCountItem> scopeDistribution;
    private List<TopPlanItem> topPlans;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RangeInfo {
        private String preset;
        private String bucket;
        private String timezone;
        private String from;
        private String to;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiValue {
        private long value;
        private Double deltaPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiBlock {
        private KpiValue totalUsers;
        private KpiValue totalCompanies;
        private KpiValue totalJobs;
        private KpiValue paidOrders;
        private KpiValue paidRevenue;
        private KpiValue activePlans;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String label;
        private long currentOrderCount;
        private long currentRevenue;
        private long previousOrderCount;
        private long previousRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCountItem {
        private String status;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScopeCountItem {
        private String scope;
        private long orders;
        private long revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPlanItem {
        private Integer planId;
        private String planName;
        private String scope;
        private long orders;
        private long paidOrders;
        private long revenue;
    }
}
