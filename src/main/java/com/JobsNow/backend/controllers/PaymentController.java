package com.JobsNow.backend.controllers;

import com.JobsNow.backend.dto.CompanySubscriptionStatusDTO;
import com.JobsNow.backend.dto.PaymentHistoryDTO;
import com.JobsNow.backend.entity.Company;
import com.JobsNow.backend.entity.PaymentOrder;
import com.JobsNow.backend.entity.SubscriptionPlan;
import com.JobsNow.backend.entity.User;
import com.JobsNow.backend.entity.enums.OrderStatus;
import com.JobsNow.backend.exception.NotFoundException;
import com.JobsNow.backend.repositories.CompanyFeatureQuotaRepository;
import com.JobsNow.backend.repositories.CompanyRepository;
import com.JobsNow.backend.repositories.PaymentOrderRepository;
import com.JobsNow.backend.repositories.SubscriptionPlanRepository;
import com.JobsNow.backend.repositories.UserRepository;
import com.JobsNow.backend.request.CreatePaymentRequest;
import com.JobsNow.backend.response.ResponseFactory;
import com.JobsNow.backend.service.VNPayService;
import com.JobsNow.backend.utils.VNPayUtils;
import com.JobsNow.backend.config.VNPayConfig;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;
    private final VNPayConfig vnPayConfig;
    private final PaymentOrderRepository orderRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyFeatureQuotaRepository quotaRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new NotFoundException("Plan not found"));

        boolean isBoost = "BOOST".equalsIgnoreCase(plan.getScope());

        if (isBoost && request.getJobId() == null) {
            return ResponseFactory.error(400, "Vui lòng chọn công việc cần boost.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        boolean hasPendingOrder = orderRepository.existsByUser_UserIdAndStatusAndPlan_Scope(user.getUserId(), OrderStatus.PENDING, plan.getScope());
        if (hasPendingOrder) {
            return ResponseFactory.error(409, "Bạn đang có giao dịch chờ thanh toán. Vui lòng hoàn tất hoặc đợi giao dịch hết hạn.", org.springframework.http.HttpStatus.CONFLICT);
        }

        if (!isBoost) {
            Company company = companyRepository.findByUser_UserId(user.getUserId()).orElse(null);
            if (company != null) {
                var quota = quotaRepository.findByCompany_CompanyId(company.getCompanyId()).orElse(null);
                var latestPaidOrder = orderRepository.findTopByUser_UserIdAndStatusAndPlan_ScopeOrderByPaidAtDesc(user.getUserId(), OrderStatus.PAID, "SUBSCRIPTION").orElse(null);
                boolean active = quota != null && quota.getExpiresAt() != null && quota.getExpiresAt().isAfter(LocalDateTime.now());
                boolean samePlanActive = active
                        && latestPaidOrder != null
                        && latestPaidOrder.getPlan() != null
                        && Objects.equals(latestPaidOrder.getPlan().getPlanId(), request.getPlanId());

                if (samePlanActive) {
                    return ResponseFactory.error(409, "Gói hiện tại vẫn còn hiệu lực. Không thể mua trùng cùng gói.", org.springframework.http.HttpStatus.CONFLICT);
                }
            }
        }

        String paymentUrl = vnPayService.createPaymentUrl(
                user.getUserId(),
                request.getPlanId(),
                request.getJobId()
        );

        Map<String, String> result = new HashMap<>();
        result.put("paymentUrl", paymentUrl);
        return ResponseFactory.success(result);
    }

    @GetMapping("/vnpay-return")
    public void handleVNPayCallback(
            @RequestParam Map<String, String> allParams,
            HttpServletResponse response) throws IOException {

        Map<String, String> paramsToVerify = new HashMap<>(allParams);
        String secureHash = paramsToVerify.remove("vnp_SecureHash");
        paramsToVerify.remove("vnp_SecureHashType");

        String txnRef = paramsToVerify.get("vnp_TxnRef");
        String responseCode = paramsToVerify.get("vnp_ResponseCode");

        if (!VNPayUtils.isValidSignature(paramsToVerify, vnPayConfig.getVnpHashSecret(), secureHash)) {
            response.sendRedirect("http://localhost:5173/payment-result?status=invalid&txnRef=" + txnRef);
            return;
        }

        vnPayService.handlePaymentCallback(paramsToVerify);

        if ("00".equals(responseCode)) {
            response.sendRedirect("http://localhost:5173/payment-result?status=success&txnRef=" + txnRef);
        } else {
            response.sendRedirect("http://localhost:5173/payment-result?status=failed&txnRef=" + txnRef);
        }
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<?> handleVNPayIpn(@RequestParam Map<String, String> allParams) {
        Map<String, String> paramsToVerify = new HashMap<>(allParams);
        String secureHash = paramsToVerify.remove("vnp_SecureHash");
        paramsToVerify.remove("vnp_SecureHashType");

        if (!VNPayUtils.isValidSignature(paramsToVerify, vnPayConfig.getVnpHashSecret(), secureHash)) {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid signature"));
        }

        try {
            vnPayService.handlePaymentCallback(paramsToVerify);
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        } catch (NotFoundException e) {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknown error"));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getPaymentHistory(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<PaymentOrder> orders = orderRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());

        List<PaymentHistoryDTO> history = orders.stream().map(order ->
                PaymentHistoryDTO.builder()
                        .orderId(order.getOrderId())
                        .orderNumber(order.getOrderNumber())
                        .planName(order.getPlan().getName())
                        .planPriorityLevel(order.getPlan().getPriorityLevel())
                        .jobTitle(order.getJob() != null ? order.getJob().getTitle() : null)
                        .totalAmount(order.getTotalAmount())
                        .status(order.getStatus())
                        .createdAt(order.getCreatedAt())
                        .paidAt(order.getPaidAt())
                        .build()
        ).collect(Collectors.toList());

        return ResponseFactory.success(history);
    }

        @GetMapping("/subscription-status")
        public ResponseEntity<?> getSubscriptionStatus(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found"));

        Company company = companyRepository.findByUser_UserId(user.getUserId())
            .orElseThrow(() -> new NotFoundException("Company not found"));

        var quota = quotaRepository.findByCompany_CompanyId(company.getCompanyId()).orElse(null);
        var latestPaidOrder = orderRepository.findTopByUser_UserIdAndStatusAndPlan_ScopeOrderByPaidAtDesc(user.getUserId(), OrderStatus.PAID, "SUBSCRIPTION").orElse(null);
        boolean hasPendingOrder = orderRepository.existsByUser_UserIdAndStatusAndPlan_Scope(user.getUserId(), OrderStatus.PENDING, "SUBSCRIPTION");

        boolean active = quota != null && quota.getExpiresAt() != null && quota.getExpiresAt().isAfter(LocalDateTime.now());
        Integer currentPlanId = latestPaidOrder != null && latestPaidOrder.getPlan() != null ? latestPaidOrder.getPlan().getPlanId() : null;
        boolean canRepurchase = !hasPendingOrder;
        String accountStatus;
        if (hasPendingOrder) {
            accountStatus = "PENDING_PAYMENT";
        } else if (active && latestPaidOrder != null) {
            accountStatus = "PAID_ACTIVE";
        } else if (active) {
            accountStatus = "TRIAL_ACTIVE";
        } else if (latestPaidOrder != null) {
            accountStatus = "EXPIRED";
        } else {
            accountStatus = "TRIAL_EXPIRED";
        }

        CompanySubscriptionStatusDTO dto = CompanySubscriptionStatusDTO.builder()
            .accountStatus(accountStatus)
            .currentPlanId(currentPlanId)
            .currentPlanName(latestPaidOrder != null && latestPaidOrder.getPlan() != null ? latestPaidOrder.getPlan().getName() : null)
            .currentPlanType(latestPaidOrder != null && latestPaidOrder.getPlan() != null && latestPaidOrder.getPlan().getType() != null
                ? latestPaidOrder.getPlan().getType().name() : null)
            .active(active)
            .startedAt(latestPaidOrder != null ? latestPaidOrder.getPaidAt() : null)
            .expiresAt(quota != null ? quota.getExpiresAt() : null)
            .remainingJobPosts(quota != null && quota.getRemainingJobPosts() != null ? quota.getRemainingJobPosts() : 0)
            .remainingAiScans(quota != null && quota.getRemainingAiScans() != null ? quota.getRemainingAiScans() : 0)
            .aiCvBuilderEnabled(quota != null && Boolean.TRUE.equals(quota.getAiCvBuilderEnabled()))
            .remainingAiCvBuilderTrials(quota != null && quota.getRemainingAiCvBuilderTrials() != null ? quota.getRemainingAiCvBuilderTrials() : 0)
            .canRepurchase(canRepurchase)
            .hasPendingOrder(hasPendingOrder)
            .build();

        return ResponseFactory.success(dto);
        }
}
