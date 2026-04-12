package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.PaymentOrder;
import com.JobsNow.backend.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Integer> {
    Optional<PaymentOrder> findByOrderNumber(String orderNumber);
    List<PaymentOrder> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
    Optional<PaymentOrder> findTopByUser_UserIdAndStatusOrderByPaidAtDesc(Integer userId, OrderStatus status);
    Optional<PaymentOrder> findTopByUser_UserIdAndStatusAndPlan_ScopeOrderByPaidAtDesc(Integer userId, OrderStatus status, String scope);
    boolean existsByUser_UserIdAndStatus(Integer userId, OrderStatus status);
    boolean existsByUser_UserIdAndStatusAndPlan_Scope(Integer userId, OrderStatus status, String scope);
}
