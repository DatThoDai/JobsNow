package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.Job;
import com.JobsNow.backend.entity.PaymentOrder;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.entity.enums.OrderStatus;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.repositories.PaymentOrderRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.response.AdminDashboardMetricsResponse;
import com.JobsNow.backend.service.AdminDashboardMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminDashboardMetricsServiceImpl implements AdminDashboardMetricsService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final PaymentOrderRepository paymentOrderRepository;

    @Override
    public AdminDashboardMetricsResponse getMetrics(
            String preset,
            String from,
            String to,
            String timezone,
            boolean comparePrevious
    ) {
        ZoneId zoneId = resolveZoneId(timezone);
        TimeRange currentRange = resolveRange(preset, from, to, zoneId);
        TimeRange previousRange = comparePrevious ? previousRange(currentRange) : null;
        BucketType bucketType = resolveBucketType(currentRange, preset);

        long totalUsersCurrent = userRepository.countByCreatedAtBetween(
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        long totalUsersPrevious = previousRange == null ? 0 : userRepository.countByCreatedAtBetween(
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );

        long totalCompaniesCurrent = companyRepository.countCreatedInRange(
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        long totalCompaniesPrevious = previousRange == null ? 0 : companyRepository.countCreatedInRange(
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );

        long totalJobsCurrent = jobRepository.countByPostedAtBetweenAndIsActiveTrueAndIsDeletedFalseAndIsApprovedTrue(
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        long totalJobsPrevious = previousRange == null ? 0 : jobRepository.countByPostedAtBetweenAndIsActiveTrueAndIsDeletedFalseAndIsApprovedTrue(
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );

        long activePlansCurrent = paymentOrderRepository.countDistinctPlansByStatusAndCreatedAtBetween(
                OrderStatus.PAID,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        long activePlansPrevious = previousRange == null ? 0 : paymentOrderRepository.countDistinctPlansByStatusAndCreatedAtBetween(
                OrderStatus.PAID,
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );

        long paidOrdersCurrent = paymentOrderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.PAID,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        long paidOrdersPrevious = previousRange == null ? 0 : paymentOrderRepository.countByStatusAndCreatedAtBetween(
                OrderStatus.PAID,
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );

        long paidRevenueCurrent = Math.round(nvlDouble(paymentOrderRepository.sumRevenueByStatusInRange(
                OrderStatus.PAID,
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        )));
        long paidRevenuePrevious = previousRange == null ? 0 : Math.round(nvlDouble(paymentOrderRepository.sumRevenueByStatusInRange(
                OrderStatus.PAID,
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        )));

        List<AdminDashboardMetricsResponse.StatusCountItem> orderStatusDistribution = buildOrderStatusDistribution(currentRange);
        List<AdminDashboardMetricsResponse.ScopeCountItem> scopeDistribution = buildScopeDistribution(currentRange);
        List<AdminDashboardMetricsResponse.TopPlanItem> topPlans = buildTopPlans(currentRange);
        List<AdminDashboardMetricsResponse.TrendPoint> trend = buildTrend(currentRange, previousRange, bucketType, comparePrevious);

        return AdminDashboardMetricsResponse.builder()
                .range(AdminDashboardMetricsResponse.RangeInfo.builder()
                        .preset(normalizePreset(preset).name().toLowerCase(Locale.ROOT))
                        .bucket(bucketType.name().toLowerCase(Locale.ROOT))
                        .timezone(zoneId.getId())
                        .from(currentRange.start.toOffsetDateTime().toString())
                        .to(currentRange.end.toOffsetDateTime().toString())
                        .build())
                .kpis(AdminDashboardMetricsResponse.KpiBlock.builder()
                        .totalUsers(kpi(totalUsersCurrent, totalUsersPrevious, comparePrevious))
                        .totalCompanies(kpi(totalCompaniesCurrent, totalCompaniesPrevious, comparePrevious))
                        .totalJobs(kpi(totalJobsCurrent, totalJobsPrevious, comparePrevious))
                        .activePlans(kpi(activePlansCurrent, activePlansPrevious, comparePrevious))
                        .paidOrders(kpi(paidOrdersCurrent, paidOrdersPrevious, comparePrevious))
                        .paidRevenue(kpi(paidRevenueCurrent, paidRevenuePrevious, comparePrevious))
                        .build())
                .trend(trend)
                .orderStatusDistribution(orderStatusDistribution)
                .scopeDistribution(scopeDistribution)
                .topPlans(topPlans)
                .build();
    }

    private List<AdminDashboardMetricsResponse.StatusCountItem> buildOrderStatusDistribution(TimeRange range) {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (Object[] row : paymentOrderRepository.countByStatusInRange(range.start.toLocalDateTime(), range.end.toLocalDateTime())) {
            OrderStatus status = row[0] == null ? null : (OrderStatus) row[0];
            if (status != null) counts.put(status, ((Number) row[1]).longValue());
        }
        List<AdminDashboardMetricsResponse.StatusCountItem> out = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            out.add(AdminDashboardMetricsResponse.StatusCountItem.builder()
                    .status(status.name())
                    .count(counts.getOrDefault(status, 0L))
                    .build());
        }
        return out;
    }

    private List<AdminDashboardMetricsResponse.ScopeCountItem> buildScopeDistribution(TimeRange range) {
        Map<String, AdminDashboardMetricsResponse.ScopeCountItem> byScope = new HashMap<>();
        for (Object[] row : paymentOrderRepository.summarizeByScopeInRange(range.start.toLocalDateTime(), range.end.toLocalDateTime())) {
            String scope = row[0] == null ? "UNKNOWN" : String.valueOf(row[0]);
            long orders = row[1] == null ? 0L : ((Number) row[1]).longValue();
            long revenue = Math.round(row[2] == null ? 0d : ((Number) row[2]).doubleValue());
            byScope.put(scope, AdminDashboardMetricsResponse.ScopeCountItem.builder()
                    .scope(scope)
                    .orders(orders)
                    .revenue(revenue)
                    .build());
        }

        List<String> expectedScopes = List.of("SUBSCRIPTION", "CANDIDATE_SUBSCRIPTION", "BOOST");
        List<AdminDashboardMetricsResponse.ScopeCountItem> out = new ArrayList<>();
        for (String scope : expectedScopes) {
            out.add(byScope.getOrDefault(scope, AdminDashboardMetricsResponse.ScopeCountItem.builder()
                    .scope(scope)
                    .orders(0)
                    .revenue(0)
                    .build()));
        }
        return out;
    }

    private List<AdminDashboardMetricsResponse.TopPlanItem> buildTopPlans(TimeRange range) {
        List<AdminDashboardMetricsResponse.TopPlanItem> out = new ArrayList<>();
        List<Object[]> rows = paymentOrderRepository.topPlansInRange(range.start.toLocalDateTime(), range.end.toLocalDateTime());
        int limit = Math.min(rows.size(), 8);
        for (int i = 0; i < limit; i++) {
            Object[] row = rows.get(i);
            out.add(AdminDashboardMetricsResponse.TopPlanItem.builder()
                    .planId(row[0] == null ? null : ((Number) row[0]).intValue())
                    .planName(row[1] == null ? "Unknown plan" : String.valueOf(row[1]))
                    .scope(row[2] == null ? "UNKNOWN" : String.valueOf(row[2]))
                    .orders(row[3] == null ? 0L : ((Number) row[3]).longValue())
                    .paidOrders(row[4] == null ? 0L : ((Number) row[4]).longValue())
                    .revenue(Math.round(row[5] == null ? 0d : ((Number) row[5]).doubleValue()))
                    .build());
        }
        return out;
    }

    private List<AdminDashboardMetricsResponse.TrendPoint> buildTrend(
            TimeRange currentRange,
            TimeRange previousRange,
            BucketType bucketType,
            boolean comparePrevious
    ) {
        List<TimeBucket> currentBuckets = buildBuckets(currentRange, bucketType);
        List<TimeBucket> previousBuckets = previousRange == null ? List.of() : buildBuckets(previousRange, bucketType);

        List<User> currentUsers = userRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        List<User> previousUsers = previousRange == null ? List.of() : userRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );
        List<java.time.LocalDateTime> currentCompanyCreatedAtValues = companyRepository.findCreatedAtValuesInRange(
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        List<java.time.LocalDateTime> previousCompanyCreatedAtValues = previousRange == null ? List.of() : companyRepository.findCreatedAtValuesInRange(
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );
        List<Job> currentJobs = jobRepository.findApprovedActivePostedInRange(
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        List<Job> previousJobs = previousRange == null ? List.of() : jobRepository.findApprovedActivePostedInRange(
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );
        List<PaymentOrder> currentOrders = paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(
                currentRange.start.toLocalDateTime(),
                currentRange.end.toLocalDateTime()
        );
        List<PaymentOrder> previousOrders = previousRange == null ? List.of() : paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(
                previousRange.start.toLocalDateTime(),
                previousRange.end.toLocalDateTime()
        );

        long[] currentCount = new long[currentBuckets.size()];
        long[] currentRevenue = new long[currentBuckets.size()];
        long[] previousCount = new long[currentBuckets.size()];
        long[] previousRevenue = new long[currentBuckets.size()];
        long[] currentUsersCount = new long[currentBuckets.size()];
        long[] previousUsersCount = new long[currentBuckets.size()];
        long[] currentCompaniesCount = new long[currentBuckets.size()];
        long[] previousCompaniesCount = new long[currentBuckets.size()];
        long[] currentJobsCount = new long[currentBuckets.size()];
        long[] previousJobsCount = new long[currentBuckets.size()];
        long[] currentActivePlansCount = new long[currentBuckets.size()];
        long[] previousActivePlansCount = new long[currentBuckets.size()];
        List<Set<Integer>> currentActivePlansSets = new ArrayList<>();
        List<Set<Integer>> previousActivePlansSets = new ArrayList<>();
        for (int i = 0; i < currentBuckets.size(); i++) {
            currentActivePlansSets.add(new HashSet<>());
            previousActivePlansSets.add(new HashSet<>());
        }

        for (User user : currentUsers) {
            if (user.getCreatedAt() == null) continue;
            int idx = findBucketIndex(currentBuckets, user.getCreatedAt().atZone(currentRange.start.getZone()));
            if (idx >= 0) currentUsersCount[idx]++;
        }
        for (java.time.LocalDateTime companyCreatedAt : currentCompanyCreatedAtValues) {
            if (companyCreatedAt == null) continue;
            int idx = findBucketIndex(currentBuckets, companyCreatedAt.atZone(currentRange.start.getZone()));
            if (idx >= 0) currentCompaniesCount[idx]++;
        }
        for (Job job : currentJobs) {
            if (job.getPostedAt() == null) continue;
            int idx = findBucketIndex(currentBuckets, job.getPostedAt().atZone(currentRange.start.getZone()));
            if (idx >= 0) currentJobsCount[idx]++;
        }

        for (PaymentOrder order : currentOrders) {
            int idx = findBucketIndex(currentBuckets, order.getCreatedAt().atZone(currentRange.start.getZone()));
            if (idx >= 0) {
                currentCount[idx]++;
                if (order.getStatus() == OrderStatus.PAID) {
                    currentRevenue[idx] += Math.round(nvlDouble(order.getTotalAmount()));
                    if (order.getPlan() != null && order.getPlan().getPlanId() != null) {
                        currentActivePlansSets.get(idx).add(order.getPlan().getPlanId());
                    }
                }
            }
        }
        for (int i = 0; i < currentActivePlansCount.length; i++) {
            currentActivePlansCount[i] = currentActivePlansSets.get(i).size();
        }
        if (comparePrevious) {
            for (User user : previousUsers) {
                if (user.getCreatedAt() == null) continue;
                int idx = findBucketIndex(previousBuckets, user.getCreatedAt().atZone(previousRange.start.getZone()));
                if (idx >= 0 && idx < previousUsersCount.length) previousUsersCount[idx]++;
            }
            for (java.time.LocalDateTime companyCreatedAt : previousCompanyCreatedAtValues) {
                if (companyCreatedAt == null) continue;
                int idx = findBucketIndex(previousBuckets, companyCreatedAt.atZone(previousRange.start.getZone()));
                if (idx >= 0 && idx < previousCompaniesCount.length) previousCompaniesCount[idx]++;
            }
            for (Job job : previousJobs) {
                if (job.getPostedAt() == null) continue;
                int idx = findBucketIndex(previousBuckets, job.getPostedAt().atZone(previousRange.start.getZone()));
                if (idx >= 0 && idx < previousJobsCount.length) previousJobsCount[idx]++;
            }
            for (PaymentOrder order : previousOrders) {
                int idx = findBucketIndex(previousBuckets, order.getCreatedAt().atZone(previousRange.start.getZone()));
                if (idx >= 0 && idx < previousCount.length) {
                    previousCount[idx]++;
                    if (order.getStatus() == OrderStatus.PAID) {
                        previousRevenue[idx] += Math.round(nvlDouble(order.getTotalAmount()));
                        if (order.getPlan() != null && order.getPlan().getPlanId() != null) {
                            previousActivePlansSets.get(idx).add(order.getPlan().getPlanId());
                        }
                    }
                }
            }
            for (int i = 0; i < previousActivePlansCount.length; i++) {
                previousActivePlansCount[i] = previousActivePlansSets.get(i).size();
            }
        }

        List<AdminDashboardMetricsResponse.TrendPoint> out = new ArrayList<>();
        for (int i = 0; i < currentBuckets.size(); i++) {
            out.add(AdminDashboardMetricsResponse.TrendPoint.builder()
                    .label(currentBuckets.get(i).label)
                    .currentOrderCount(currentCount[i])
                    .currentRevenue(currentRevenue[i])
                    .previousOrderCount(comparePrevious ? previousCount[i] : 0)
                    .previousRevenue(comparePrevious ? previousRevenue[i] : 0)
                    .currentTotalUsers(currentUsersCount[i])
                    .previousTotalUsers(comparePrevious ? previousUsersCount[i] : 0)
                    .currentTotalCompanies(currentCompaniesCount[i])
                    .previousTotalCompanies(comparePrevious ? previousCompaniesCount[i] : 0)
                    .currentTotalJobs(currentJobsCount[i])
                    .previousTotalJobs(comparePrevious ? previousJobsCount[i] : 0)
                    .currentActivePlans(currentActivePlansCount[i])
                    .previousActivePlans(comparePrevious ? previousActivePlansCount[i] : 0)
                    .build());
        }
        return out;
    }

    private AdminDashboardMetricsResponse.KpiValue kpi(long current, Long previous, boolean withDelta) {
        return AdminDashboardMetricsResponse.KpiValue.builder()
                .value(current)
                .deltaPercent(withDelta && previous != null ? delta(current, previous) : null)
                .build();
    }

    private Double delta(long current, long previous) {
        if (previous <= 0) return current <= 0 ? 0d : 100d;
        return Math.round((((current - previous) * 100d) / previous) * 100d) / 100d;
    }

    private ZoneId resolveZoneId(String timezone) {
        String value = (timezone == null || timezone.isBlank()) ? "Asia/Ho_Chi_Minh" : timezone.trim();
        try {
            return ZoneId.of(value);
        } catch (Exception ex) {
            return ZoneId.of("Asia/Ho_Chi_Minh");
        }
    }

    private TimeRange resolveRange(String presetRaw, String from, String to, ZoneId zoneId) {
        PresetType preset = normalizePreset(presetRaw);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return switch (preset) {
            case DAY -> new TimeRange(
                    now.toLocalDate().atStartOfDay(zoneId),
                    now.toLocalDate().plusDays(1).atStartOfDay(zoneId).minusNanos(1)
            );
            case MONTH -> {
                LocalDate startDate = now.toLocalDate().withDayOfMonth(1);
                yield new TimeRange(startDate.atStartOfDay(zoneId), startDate.plusMonths(1).atStartOfDay(zoneId).minusNanos(1));
            }
            case YEAR -> {
                LocalDate startDate = now.toLocalDate().withDayOfYear(1);
                yield new TimeRange(startDate.atStartOfDay(zoneId), startDate.plusYears(1).atStartOfDay(zoneId).minusNanos(1));
            }
            case CUSTOM -> {
                if (from == null || from.isBlank() || to == null || to.isBlank()) {
                    throw new BadRequestException("from and to are required for custom preset");
                }
                LocalDate startDate = parseDate(from);
                LocalDate endDate = parseDate(to);
                if (endDate.isBefore(startDate)) {
                    throw new BadRequestException("to must be greater than or equal to from");
                }
                long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
                if (days > 366) {
                    throw new BadRequestException("custom date range cannot exceed 366 days");
                }
                yield new TimeRange(
                        startDate.atStartOfDay(zoneId),
                        endDate.plusDays(1).atStartOfDay(zoneId).minusNanos(1)
                );
            }
        };
    }

    private PresetType normalizePreset(String value) {
        if (value == null || value.isBlank()) return PresetType.MONTH;
        try {
            return PresetType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("Invalid preset. Allowed: day, month, year, custom");
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid date format. Expected yyyy-MM-dd");
        }
    }

    private TimeRange previousRange(TimeRange currentRange) {
        long nanos = Math.max(1, currentRange.nanos());
        ZonedDateTime prevEnd = currentRange.start.minusNanos(1);
        ZonedDateTime prevStart = prevEnd.minusNanos(nanos - 1);
        return new TimeRange(prevStart, prevEnd);
    }

    private BucketType resolveBucketType(TimeRange range, String presetRaw) {
        PresetType preset = normalizePreset(presetRaw);
        if (preset == PresetType.DAY) return BucketType.HOUR;
        if (preset == PresetType.MONTH) return BucketType.DAY;
        if (preset == PresetType.YEAR) return BucketType.MONTH;
        long days = range.daySpan();
        if (days <= 31) return BucketType.DAY;
        if (days <= 180) return BucketType.WEEK;
        return BucketType.MONTH;
    }

    private List<TimeBucket> buildBuckets(TimeRange range, BucketType bucketType) {
        List<TimeBucket> buckets = new ArrayList<>();
        ZoneId zoneId = range.start.getZone();
        switch (bucketType) {
            case HOUR -> {
                ZonedDateTime cursor = range.start;
                for (int i = 0; i < 24; i++) {
                    ZonedDateTime next = cursor.plusHours(1);
                    buckets.add(new TimeBucket(cursor, next.minusNanos(1), cursor.format(DateTimeFormatter.ofPattern("HH:mm"))));
                    cursor = next;
                }
            }
            case DAY -> {
                ZonedDateTime cursor = range.start.toLocalDate().atStartOfDay(zoneId);
                ZonedDateTime endExclusive = range.end.plusNanos(1);
                while (cursor.isBefore(endExclusive)) {
                    ZonedDateTime next = cursor.plusDays(1);
                    if (next.isAfter(endExclusive)) next = endExclusive;
                    buckets.add(new TimeBucket(cursor, next.minusNanos(1), cursor.format(DateTimeFormatter.ofPattern("dd/MM"))));
                    cursor = next;
                }
            }
            case WEEK -> {
                ZonedDateTime cursor = range.start.toLocalDate().atStartOfDay(zoneId);
                ZonedDateTime endExclusive = range.end.plusNanos(1);
                int idx = 1;
                while (cursor.isBefore(endExclusive)) {
                    ZonedDateTime next = cursor.plusDays(7);
                    if (next.isAfter(endExclusive)) next = endExclusive;
                    buckets.add(new TimeBucket(cursor, next.minusNanos(1), "W" + idx));
                    cursor = next;
                    idx++;
                }
            }
            case MONTH -> {
                YearMonth cursor = YearMonth.from(range.start);
                YearMonth endYm = YearMonth.from(range.end);
                while (!cursor.isAfter(endYm)) {
                    ZonedDateTime start = cursor.atDay(1).atStartOfDay(zoneId);
                    ZonedDateTime next = cursor.plusMonths(1).atDay(1).atStartOfDay(zoneId);
                    ZonedDateTime end = next.minusNanos(1);
                    if (start.isBefore(range.start)) start = range.start;
                    if (end.isAfter(range.end)) end = range.end;
                    buckets.add(new TimeBucket(start, end, "T" + cursor.getMonthValue()));
                    cursor = cursor.plusMonths(1);
                }
            }
        }
        return buckets;
    }

    private int findBucketIndex(List<TimeBucket> buckets, ZonedDateTime value) {
        for (int i = 0; i < buckets.size(); i++) {
            TimeBucket bucket = buckets.get(i);
            if (!value.isBefore(bucket.start) && !value.isAfter(bucket.end)) return i;
        }
        return -1;
    }

    private double nvlDouble(Double value) {
        return value == null ? 0d : value;
    }

    private enum PresetType {
        DAY,
        MONTH,
        YEAR,
        CUSTOM
    }

    private enum BucketType {
        HOUR,
        DAY,
        WEEK,
        MONTH
    }

    private record TimeRange(ZonedDateTime start, ZonedDateTime end) {
        long daySpan() {
            return Duration.between(start, end).toDays() + 1;
        }

        long nanos() {
            return Duration.between(start, end).toNanos() + 1;
        }
    }

    private record TimeBucket(ZonedDateTime start, ZonedDateTime end, String label) {}
}
