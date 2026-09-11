package com.govia.audit.planengagement.processengagement.controller;

import com.govia.audit.planengagement.dto.AuditWorkManagementItemResponse;
import com.govia.audit.planengagement.monitoring.dto.AuditEngagementMonitoringResponse;
import com.govia.audit.planengagement.processengagement.service.AuditProcessEngagementAggregateService;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationResponse;
import com.govia.audit.planengagement.ttss.dto.AuditTtssRecordResponse;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 4 man hinh doc-tong-hop cua "QL CKT quy trinh" (Tao CKT (4).xlsx): "Chi tiết đoàn", "Quản lý
 * công việc", "Quản lý TTSS", "Quản lý KN" - deu chi VIEW, tai su dung permission cua chinh module
 * CKT quy trinh (khong tao permission rieng). */
@RestController
@RequestMapping("/api/audit/plan/engagement-process/{processEngagementId}")
public class AuditProcessEngagementAggregateController {

    private final AuditProcessEngagementAggregateService service;

    public AuditProcessEngagementAggregateController(AuditProcessEngagementAggregateService service) {
        this.service = service;
    }

    @GetMapping("/children")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.VIEW')")
    public ApiResponse<List<AuditEngagementMonitoringResponse>> children(@PathVariable UUID processEngagementId) {
        return ApiResponse.ok(service.children(processEngagementId));
    }

    @GetMapping("/work-management")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.VIEW')")
    public ApiResponse<List<AuditWorkManagementItemResponse>> workManagement(@PathVariable UUID processEngagementId,
                                                                               @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.workManagement(processEngagementId, principal));
    }

    @GetMapping("/ttss")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.VIEW')")
    public ApiResponse<List<AuditTtssRecordResponse>> ttss(@PathVariable UUID processEngagementId) {
        return ApiResponse.ok(service.ttss(processEngagementId));
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.VIEW')")
    public ApiResponse<List<AuditRecommendationResponse>> recommendations(@PathVariable UUID processEngagementId) {
        return ApiResponse.ok(service.recommendations(processEngagementId));
    }
}
