package com.JobsNow.backend.service.imp;

import com.JobsNow.backend.config.VNPayConfig;
import com.JobsNow.backend.entity.*;
import com.JobsNow.backend.entity.enums.PlanType;
import com.JobsNow.backend.entity.enums.JobHotTag;
import com.JobsNow.backend.entity.enums.OrderStatus;
import com.JobsNow.backend.exception.BadRequestException;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.repositories.*;
import com.JobsNow.backend.service.CompanyQuotaService;
import com.JobsNow.backend.service.CandidateQuotaService;
import com.JobsNow.backend.service.VNPayService;
import com.JobsNow.backend.service.JobService;
import com.JobsNow.backend.utils.VNPayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {

    private static final double MAX_BOOST_CAP = 0.6;

    private final VNPayConfig vnPayConfig;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PaymentOrderRepository orderRepository;
    private final JobRepository jobRepository;
    private final JobBoostRepository jobBoostRepository;
    private final CompanyQuotaService companyQuotaService;
    private final CandidateQuotaService candidateQuotaService;
    private final CompanyRepository companyRepository;
    private final JobService jobService;

    @Override
    @Transactional
    public String createPaymentUrl(Integer userId, Integer planId, Integer jobId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Plan not found"));

        boolean isBoost = "BOOST".equalsIgnoreCase(plan.getScope());

        Job job = null;
        if (isBoost) {
            if (jobId == null) {
                throw new BadRequestException("Vui lòng chọn công việc cần boost");
            }
            job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new NotFoundException("Job not found"));
        } else if (jobId != null) {
            job = jobRepository.findById(jobId).orElse(null);
        }

        PaymentOrder order = PaymentOrder.builder()
                .user(user)
                .plan(plan)
                .job(job)
                .orderNumber(generateOrderNumber())
                .totalAmount(plan.getPrice())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getVnpTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf((long) (plan.getPrice() * 100)));
        vnpParams.put("vnp_BankCode", "NCB");
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", order.getOrderNumber());
        vnpParams.put("vnp_OrderInfo", (isBoost ? "BoostJob_" : "MuaGoi_") + order.getOrderNumber());
        vnpParams.put("vnp_OrderType", "billpayment");
        vnpParams.put("vnp_IpAddr", vnPayConfig.getVnpIpAddr());
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnpParams.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        return VNPayUtils.buildQuery(vnpParams, vnPayConfig.getVnpHashSecret(), vnPayConfig.getVnpUrl());
    }

    @Override
    @Transactional
    public void handlePaymentCallback(Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");

        PaymentOrder order = orderRepository.findByOrderNumber(txnRef)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.PAID) {
            return;
        }

        if ("00".equals(responseCode)) {
            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            order.setVnpTransactionNo(transactionNo);
            orderRepository.save(order);

            SubscriptionPlan plan = order.getPlan();
            boolean isBoost = "BOOST".equalsIgnoreCase(plan.getScope());

            if (isBoost) {
                activateJobBoost(order);
            } else {
                activateSubscription(order);
            }

            log.info("Payment successful for order: {}", txnRef);
        } else {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            log.warn("Payment failed for order: {} with code: {}", txnRef, responseCode);
        }
    }

    private void activateSubscription(PaymentOrder order) {
        SubscriptionPlan plan = order.getPlan();
        User user = order.getUser();

        if ("CANDIDATE".equalsIgnoreCase(plan.getTargetAudience())) {
            candidateQuotaService.grantPlanBenefits(user.getUserId(), plan);
        } else {
            companyRepository.findByUser_UserId(user.getUserId()).ifPresent(company ->
                    companyQuotaService.grantPlanBenefits(company.getCompanyId(), plan)
            );
        }

        log.info("Subscription activated for user: {}, plan: {}", user.getUserId(), plan.getName());
    }

    private void activateJobBoost(PaymentOrder order) {
        SubscriptionPlan plan = order.getPlan();
        Job job = order.getJob();

        if (job == null) {
            log.warn("Boost order {} has no job associated", order.getOrderNumber());
            return;
        }

        Optional<JobBoost> existingBoost = jobBoostRepository.findByJob_JobIdAndIsActiveTrue(job.getJobId());
        existingBoost.ifPresent(boost -> {
            boost.setIsActive(false);
            jobBoostRepository.save(boost);
        });

        LocalDateTime now = LocalDateTime.now();
        JobBoost boost = JobBoost.builder()
                .job(job)
                .plan(plan)
                .order(order)
                .boostScore(plan.getBoostScore())
                .startAt(now)
                .endAt(now.plusDays(plan.getDurationDays()))
                .isActive(true)
                .build();

        jobBoostRepository.save(boost);

        job.setBoostScore(plan.getBoostScore());

        double baseScore = job.getBaseScore() != null ? job.getBaseScore() : 0.0;
        double boostScore = job.getBoostScore() != null ? job.getBoostScore() : 0.0;
        double effectiveBoost = Math.max(0.0, Math.min(boostScore, MAX_BOOST_CAP));
        double finalScore = Math.min(1.0, (0.7 * baseScore) + (0.3 * effectiveBoost));

        JobHotTag tag = JobHotTag.NORMAL;
        if (plan.getType() == PlanType.VIP) {
            tag = JobHotTag.SUPER_HOT;
        } else if (boostScore >= 0.8) {
            tag = JobHotTag.SUPER_HOT;
        } else if (boostScore >= 0.3) {
            tag = JobHotTag.HOT;
        } else if (baseScore >= 0.7) {
            tag = JobHotTag.SUPER_HOT;
        } else if (baseScore >= 0.5 || boostScore > 0) {
            tag = JobHotTag.HOT;
        }

        job.setFinalScore(finalScore);
        job.setHotTag(tag);

        jobRepository.save(job);
        log.info("Boost activated for job: {}, plan: {}", job.getJobId(), plan.getName());

        try {
            jobService.pushSingleJobToAlgolia(job.getJobId());
            log.info("Synced newly boosted job to Algolia.");
        } catch (Exception e) {
            log.warn("Failed to sync boosted job to Algolia", e);
        }
    }

    private String generateOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        return "VIP" + now.getYear()
                + String.format("%02d", now.getMonthValue())
                + String.format("%02d", now.getDayOfMonth())
                + String.format("%02d", now.getHour())
                + String.format("%02d", now.getMinute())
                + String.format("%02d", now.getSecond())
                + String.format("%03d", new Random().nextInt(1000));
    }
}
