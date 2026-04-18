package com.JobsNow.backend.repositories;

import com.JobsNow.backend.entity.SubscriptionPlan;
import com.JobsNow.backend.entity.enums.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Integer> {
    List<SubscriptionPlan> findByIsActiveTrue();
    List<SubscriptionPlan> findByIsActiveTrueOrderByPriorityLevelDescPriceAsc();
    List<SubscriptionPlan> findByIsActiveTrueAndScopeOrderByPriorityLevelDescPriceAsc(String scope);
    Optional<SubscriptionPlan> findByType(PlanType type);
    long countByIsActiveTrue();
    long countByIsActiveTrueAndScope(String scope);
}
