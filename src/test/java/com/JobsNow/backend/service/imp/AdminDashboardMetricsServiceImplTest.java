package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.entity.PaymentOrder;
import com.JobsNow.backend.entity.SubscriptionPlan;
import com.JobsNow.backend.entity.enums.OrderStatus;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.repositories.PaymentOrderRepository;
import com.JobsNow.backend.repositories.SubscriptionPlanRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.response.AdminDashboardMetricsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardMetricsServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private JobRepository jobRepository;
    @Mock private SubscriptionPlanRepository subscriptionPlanRepository;
    @Mock private PaymentOrderRepository paymentOrderRepository;

    @InjectMocks
    private AdminDashboardMetricsServiceImpl service;

    @Test
    void shouldThrowWhenPresetInvalid() {
        assertThrows(BadRequestException.class, () ->
                service.getMetrics("bad", null, null, "Asia/Ho_Chi_Minh", true)
        );
    }

    @Test
    void shouldThrowWhenCustomMissingDates() {
        assertThrows(BadRequestException.class, () ->
                service.getMetrics("custom", null, null, "Asia/Ho_Chi_Minh", true)
        );
    }

    @Test
    void shouldBuildMetricsForMonthPreset() {
        when(userRepository.count()).thenReturn(100L);
        when(companyRepository.count()).thenReturn(20L);
        when(jobRepository.count()).thenReturn(55L);
        when(subscriptionPlanRepository.countByIsActiveTrue()).thenReturn(7L);

        when(paymentOrderRepository.countByStatusAndCreatedAtBetween(any(), any(), any())).thenReturn(4L);
        when(paymentOrderRepository.sumRevenueByStatusInRange(any(), any(), any())).thenReturn(1500000d);
        when(paymentOrderRepository.countByStatusInRange(any(), any())).thenReturn(List.of(new Object[]{OrderStatus.PAID, 4L}));
        when(paymentOrderRepository.summarizeByScopeInRange(any(), any()))
                .thenReturn(List.of(new Object[]{"SUBSCRIPTION", 3L, 1200000d}));
        when(paymentOrderRepository.topPlansInRange(any(), any()))
                .thenReturn(List.of(new Object[]{1, "VIP 30D", "SUBSCRIPTION", 3L, 3L, 1200000d}));

        PaymentOrder paidOrder = PaymentOrder.builder()
                .status(OrderStatus.PAID)
                .totalAmount(500000d)
                .createdAt(LocalDateTime.now())
                .plan(SubscriptionPlan.builder().planId(1).name("VIP 30D").scope("SUBSCRIPTION").build())
                .build();
        when(paymentOrderRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(paidOrder));

        AdminDashboardMetricsResponse response = service.getMetrics("month", null, null, "Asia/Ho_Chi_Minh", false);

        assertEquals(100L, response.getKpis().getTotalUsers().getValue());
        assertEquals(20L, response.getKpis().getTotalCompanies().getValue());
        assertEquals(55L, response.getKpis().getTotalJobs().getValue());
        assertEquals(7L, response.getKpis().getActivePlans().getValue());
        assertEquals(4L, response.getKpis().getPaidOrders().getValue());
        assertEquals(1500000L, response.getKpis().getPaidRevenue().getValue());
    }
}
