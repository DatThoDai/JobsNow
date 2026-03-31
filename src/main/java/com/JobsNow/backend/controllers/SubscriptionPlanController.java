package com.JobsNow.backend.controllers;

import com.JobsNow.backend.dto.SubscriptionPlanDTO;
import com.JobsNow.backend.entity.SubscriptionPlan;
import com.JobsNow.backend.repositories.SubscriptionPlanRepository;
import com.JobsNow.backend.response.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanRepository planRepository;

    @GetMapping
    public ResponseEntity<?> getAllPlans(@RequestParam(required = false) String scope) {
        List<SubscriptionPlan> rawPlans;
        if (scope != null && !scope.isBlank()) {
            rawPlans = planRepository.findByIsActiveTrueAndScopeOrderByPriorityLevelDescPriceAsc(scope.toUpperCase());
        } else {
            rawPlans = planRepository.findByIsActiveTrueOrderByPriorityLevelDescPriceAsc();
        }
        List<SubscriptionPlanDTO> plans = rawPlans.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseFactory.success(plans);
    }

    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody SubscriptionPlanDTO dto) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(dto.getName());
        plan.setPrice(dto.getPrice());
        plan.setType(dto.getType());
        plan.setDurationDays(dto.getDurationDays());
        plan.setBoostScore(dto.getBoostScore());
        plan.setJobPostLimit(dto.getJobPostLimit());
        plan.setAiCvScanningLimit(dto.getAiCvScanningLimit());
        plan.setUseAiCvBuilder(dto.getUseAiCvBuilder());
        plan.setPriorityLevel(resolvePriorityLevel(dto));
        plan.setScope(dto.getScope() != null ? dto.getScope().toUpperCase() : "SUBSCRIPTION");
        plan.setDescription(dto.getDescription());
        plan.setIsActive(true);
        planRepository.save(plan);
        return ResponseFactory.successMessage("Plan created successfully");
    }

    @PutMapping("/{planId}")
    public ResponseEntity<?> updatePlan(@PathVariable Integer planId, @RequestBody SubscriptionPlanDTO dto) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        plan.setName(dto.getName());
        plan.setPrice(dto.getPrice());
        plan.setType(dto.getType());
        plan.setDurationDays(dto.getDurationDays());
        plan.setBoostScore(dto.getBoostScore());
        plan.setJobPostLimit(dto.getJobPostLimit());
        plan.setAiCvScanningLimit(dto.getAiCvScanningLimit());
        plan.setUseAiCvBuilder(dto.getUseAiCvBuilder());
        plan.setPriorityLevel(resolvePriorityLevel(dto));
        plan.setScope(dto.getScope() != null ? dto.getScope().toUpperCase() : plan.getScope());
        plan.setDescription(dto.getDescription());
        planRepository.save(plan);
        return ResponseFactory.successMessage("Plan updated successfully");
    }

    private SubscriptionPlanDTO toDTO(SubscriptionPlan plan) {
        return SubscriptionPlanDTO.builder()
                .planId(plan.getPlanId())
                .name(plan.getName())
                .price(plan.getPrice())
                .type(plan.getType())
                .durationDays(plan.getDurationDays())
                .boostScore(plan.getBoostScore())
                .jobPostLimit(plan.getJobPostLimit())
                .aiCvScanningLimit(plan.getAiCvScanningLimit())
                .useAiCvBuilder(plan.getUseAiCvBuilder())
                .priorityLevel(plan.getPriorityLevel())
                .scope(plan.getScope())
                .description(plan.getDescription())
                .build();
    }

    private Integer resolvePriorityLevel(SubscriptionPlanDTO dto) {
        if (dto.getPriorityLevel() != null) {
            return dto.getPriorityLevel();
        }
        if (dto.getType() == null) {
            return 0;
        }
        return switch (dto.getType()) {
            case VIP -> 3;
            case PREMIUM -> 2;
            case PLUS -> 1;
            default -> 0;
        };
    }
}
