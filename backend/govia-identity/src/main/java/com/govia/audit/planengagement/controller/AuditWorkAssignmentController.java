package com.govia.audit.planengagement.controller;

import com.govia.audit.planengagement.dto.AuditWorkAssignmentApproveRequest;
import com.govia.audit.planengagement.dto.AuditWorkAssignmentStatusUpdateRequest;
import com.govia.audit.planengagement.dto.AuditWorkManagementItemResponse;
import com.govia.audit.planengagement.service.AuditWorkAssignmentService;
import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** "Quản lý công việc" (CBKT/THKT) - sheet cung ten trong Tạo CKT (1).xlsx. */
@RestController
@RequestMapping("/api/audit/plan/engagement/{engagementId}/work-management")
public class AuditWorkAssignmentController {

    private final AuditWorkAssignmentService service;

    public AuditWorkAssignmentController(AuditWorkAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.WORK_MANAGEMENT.VIEW')")
    public ApiResponse<List<AuditWorkManagementItemResponse>> list(@PathVariable UUID engagementId,
                                                                    @RequestParam AuditWorkPhase phase,
                                                                    @RequestParam(required = false) UUID employeeId) {
        return ApiResponse.ok(service.list(engagementId, phase, employeeId));
    }

    @PutMapping("/{assignmentId}/status")
    @PreAuthorize("hasAuthority('PERM_AUDIT.WORK_MANAGEMENT.EDIT')")
    public ApiResponse<AuditWorkManagementItemResponse> updateStatus(@PathVariable UUID engagementId, @PathVariable UUID assignmentId,
                                                                      @Valid @RequestBody AuditWorkAssignmentStatusUpdateRequest request,
                                                                      @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.updateStatus(engagementId, assignmentId, request, principal.employeeCode()));
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('PERM_AUDIT.WORK_MANAGEMENT.APPROVE')")
    public ApiResponse<List<UUID>> approve(@PathVariable UUID engagementId, @Valid @RequestBody AuditWorkAssignmentApproveRequest request,
                                            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.approve(engagementId, request, principal));
    }
}
