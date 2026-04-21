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
public class CompanyDashboardMetricsResponse {
    private RangeInfo range;
    private KpiBlock kpis;
    private List<TrendPoint> trend;
    private List<RatingDistributionItem> ratingDistribution;
    private List<StatusCountItem> applicationPipeline;
    private List<StatusCountItem> postStatus;
    private List<TopJobItem> topJobs;

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
        private KpiValue followers;
        private KpiValue reviews;
        private KpiValue approvedPosts;
        private KpiValue applications;
        private KpiValue avgRatingX100;
        private KpiValue jobViews;
        private KpiValue jobApplies;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String label;
        private long currentFollowers;
        private long previousFollowers;
        private long currentApplications;
        private long previousApplications;
        private long currentReviews;
        private long previousReviews;
        private long currentApprovedPosts;
        private long previousApprovedPosts;
        private long currentJobViews;
        private long previousJobViews;
        private long currentJobApplies;
        private long previousJobApplies;
        private long currentAvgRating;
        private long previousAvgRating;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDistributionItem {
        private int star;
        private long count;
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
    public static class TopJobItem {
        private Integer jobId;
        private String title;
        private long viewCount;
        private long applyCount;
        private double conversionRate;
        private boolean active;
        private boolean approved;
    }
}
