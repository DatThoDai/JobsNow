package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.config.VNPayConfig;
import com.JobsNow.backend.entity.Job;
import com.JobsNow.backend.entity.JobBoost;
import com.JobsNow.backend.entity.PaymentOrder;
import com.JobsNow.backend.entity.SubscriptionPlan;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.entity.enums.JobHotTag;
import com.JobsNow.backend.entity.enums.OrderStatus;
import com.JobsNow.backend.entity.enums.PlanType;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.JobBoostRepository;
import com.JobsNow.backend.repositories.JobRepository;
import com.JobsNow.backend.repositories.PaymentOrderRepository;
import com.JobsNow.backend.repositories.SubscriptionPlanRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.service.CandidateQuotaService;
import com.JobsNow.backend.service.CompanyQuotaService;
import com.JobsNow.backend.service.JobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VNPayServiceImplTest {

    @Mock private VNPayConfig vnPayConfig;
    @Mock private UserRepository userRepository;
    @Mock private SubscriptionPlanRepository planRepository;
    @Mock private PaymentOrderRepository orderRepository;
    @Mock private JobRepository jobRepository;
    @Mock private JobBoostRepository jobBoostRepository;
    @Mock private CompanyQuotaService companyQuotaService;
    @Mock private CandidateQuotaService candidateQuotaService;
    @Mock private CompanyRepository companyRepository;
    @Mock private JobService jobService;

    @InjectMocks
    private VNPayServiceImpl service;

    @Test
    void handlePaymentCallback_shouldSetPaid_andActivateBoost_andSyncAlgolia() {
        User user = new User();
        user.setUserId(1);

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .planId(2)
                .name("VIP Boost")
                .scope("BOOST")
                .type(PlanType.VIP)
                .boostScore(0.8)
                .durationDays(7)
                .price(100_000d)
                .build();

        Job job = new Job();
        job.setJobId(99);
        job.setBaseScore(0.5);
        job.setBoostScore(0.0);

        PaymentOrder order = PaymentOrder.builder()
                .orderId(5)
                .orderNumber("VIP2026001")
                .status(OrderStatus.PENDING)
                .user(user)
                .plan(plan)
                .job(job)
                .build();

        when(orderRepository.findByOrderNumber("VIP2026001")).thenReturn(Optional.of(order));
        when(jobBoostRepository.findByJob_JobIdAndIsActiveTrue(99)).thenReturn(Optional.empty());

        service.handlePaymentCallback(Map.of(
                "vnp_TxnRef", "VIP2026001",
                "vnp_ResponseCode", "00",
                "vnp_TransactionNo", "TRANS123"
        ));

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertNotNull(order.getPaidAt());
        assertEquals("TRANS123", order.getVnpTransactionNo());

        verify(orderRepository, atLeastOnce()).save(order);
        verify(jobBoostRepository).save(any(JobBoost.class));
        verify(jobRepository).save(job);
        verify(jobService).pushSingleJobToAlgolia(99);

        assertNotNull(job.getFinalScore());
        assertEquals(JobHotTag.SUPER_HOT, job.getHotTag());
    }

    @Test
    void handlePaymentCallback_shouldBeIdempotent_whenOrderAlreadyPaid() {
        PaymentOrder order = PaymentOrder.builder()
                .orderNumber("VIP2026002")
                .status(OrderStatus.PAID)
                .build();

        when(orderRepository.findByOrderNumber("VIP2026002")).thenReturn(Optional.of(order));

        service.handlePaymentCallback(Map.of(
                "vnp_TxnRef", "VIP2026002",
                "vnp_ResponseCode", "00",
                "vnp_TransactionNo", "TRANS999"
        ));

        verify(jobBoostRepository, never()).save(any());
        verify(jobRepository, never()).save(any());
        verify(jobService, never()).pushSingleJobToAlgolia(anyInt());
    }
}
