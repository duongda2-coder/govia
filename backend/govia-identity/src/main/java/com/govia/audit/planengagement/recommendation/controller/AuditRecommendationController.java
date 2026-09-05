package com.govia.audit.planengagement.recommendation.controller;

import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationRequest;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationResponse;
import com.govia.audit.planengagement.recommendation.service.AuditRecommendationService;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** "3. Thêm kiến nghị" (man hinh "Quản lý TTSS & Kiến nghị"). */
@RestController
@RequestMapping("/api/audit/plan/engagement/{engagementId}/ttss/recommendations")
public class AuditRecommendationController {

    private final AuditRecommendationService service;

    public AuditRecommendationController(AuditRecommendationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.VIEW')")
    public ApiResponse<List<AuditRecommendationResponse>> list(@PathVariable UUID engagementId) {
        return ApiResponse.ok(service.list(engagementId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.EDIT')")
    public ApiResponse<AuditRecommendationResponse> create(@PathVariable UUID engagementId, @Valid @RequestBody AuditRecommendationRequest request) {
        return ApiResponse.ok(service.create(engagementId, request));
    }
}
