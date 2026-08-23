package com.govia.identity.controller;

import com.govia.core.web.ApiResponse;
import com.govia.identity.dto.ApprovalMatrixRuleRequest;
import com.govia.identity.dto.ApprovalMatrixRuleResponse;
import com.govia.identity.service.ApprovalMatrixRuleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Man hinh cau hinh "Ma tran phe duyet" - xem ApprovalMatrixRuleService de biet cach EmployeeApprovalService dung lai. */
@RestController
@RequestMapping("/api/workflow/approval-matrix")
public class ApprovalMatrixRuleController {

    private final ApprovalMatrixRuleService service;

    public ApprovalMatrixRuleController(ApprovalMatrixRuleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.APPROVAL_MATRIX.VIEW')")
    public ApiResponse<List<ApprovalMatrixRuleResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.APPROVAL_MATRIX.MANAGE')")
    public ApiResponse<ApprovalMatrixRuleResponse> create(@Valid @RequestBody ApprovalMatrixRuleRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.APPROVAL_MATRIX.MANAGE')")
    public ApiResponse<ApprovalMatrixRuleResponse> update(@PathVariable UUID id, @Valid @RequestBody ApprovalMatrixRuleRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_WORKFLOW.APPROVAL_MATRIX.MANAGE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
