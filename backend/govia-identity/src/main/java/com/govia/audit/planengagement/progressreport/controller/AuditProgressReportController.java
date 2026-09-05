package com.govia.audit.planengagement.progressreport.controller;

import com.govia.audit.planengagement.progressreport.dto.AuditProgressReportApproveRequest;
import com.govia.audit.planengagement.progressreport.dto.AuditProgressReportResponse;
import com.govia.audit.planengagement.progressreport.service.AuditProgressReportService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** "Báo cáo tiến độ" - 1 chuc nang trong man hinh Quan ly cong viec THKT, dung chung permission
 * AUDIT.WORK_MANAGEMENT.*. */
@RestController
@RequestMapping("/api/audit/plan/engagement/{engagementId}/work-management/progress-reports")
public class AuditProgressReportController {

    private final AuditProgressReportService service;

    public AuditProgressReportController(AuditProgressReportService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.WORK_MANAGEMENT.VIEW')")
    public ApiResponse<List<AuditProgressReportResponse>> list(@PathVariable UUID engagementId) {
        return ApiResponse.ok(service.list(engagementId));
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('PERM_AUDIT.WORK_MANAGEMENT.APPROVE')")
    public ApiResponse<List<UUID>> approve(@PathVariable UUID engagementId, @Valid @RequestBody AuditProgressReportApproveRequest request,
                                            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.approve(engagementId, request, principal));
    }
}
