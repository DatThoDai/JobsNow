package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.PaymentOrder;
import com.JobsNow.backend.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Integer> {
    Optional<PaymentOrder> findByOrderNumber(String orderNumber);
    List<PaymentOrder> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
    Optional<PaymentOrder> findTopByUser_UserIdAndStatusOrderByPaidAtDesc(Integer userId, OrderStatus status);
    Optional<PaymentOrder> findTopByUser_UserIdAndStatusAndPlan_ScopeOrderByPaidAtDesc(Integer userId, OrderStatus status, String scope);
    boolean existsByUser_UserIdAndStatus(Integer userId, OrderStatus status);
    boolean existsByUser_UserIdAndStatusAndPlan_Scope(Integer userId, OrderStatus status, String scope);

    List<PaymentOrder> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end);

    @Query("""
        select o.status, count(o)
        from PaymentOrder o
        where o.createdAt between :start and :end
        group by o.status
    """)
    List<Object[]> countByStatusInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        select o.plan.scope, count(o), coalesce(sum(case when o.status = com.JobsNow.backend.entity.enums.OrderStatus.PAID then o.totalAmount else 0 end), 0)
        from PaymentOrder o
        where o.createdAt between :start and :end
        group by o.plan.scope
    """)
    List<Object[]> summarizeByScopeInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        select o.plan.planId, o.plan.name, o.plan.scope, count(o),
               coalesce(sum(case when o.status = com.JobsNow.backend.entity.enums.OrderStatus.PAID then 1 else 0 end), 0),
               coalesce(sum(case when o.status = com.JobsNow.backend.entity.enums.OrderStatus.PAID then o.totalAmount else 0 end), 0)
        from PaymentOrder o
        where o.createdAt between :start and :end
        group by o.plan.planId, o.plan.name, o.plan.scope
        order by coalesce(sum(case when o.status = com.JobsNow.backend.entity.enums.OrderStatus.PAID then o.totalAmount else 0 end), 0) desc
    """)
    List<Object[]> topPlansInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    long countByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);

    @Query("""
        select coalesce(sum(o.totalAmount), 0)
        from PaymentOrder o
        where o.status = :status and o.createdAt between :start and :end
    """)
    Double sumRevenueByStatusInRange(
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
