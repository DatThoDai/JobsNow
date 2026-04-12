package com.JobsNow.backend.dto;

import com.JobsNow.backend.entity.enums.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistoryDTO {
    private Integer orderId;
    private String orderNumber;
    private String planName;
    private Integer planPriorityLevel;
    private String jobTitle;
    private Double totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
