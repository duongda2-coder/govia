package com.govia.audit.riskscoring.masterdata.controller;

import com.govia.audit.riskscoring.masterdata.dto.AuditObjectInspectionHistoryRequest;
import com.govia.audit.riskscoring.masterdata.dto.AuditObjectInspectionHistoryResponse;
import com.govia.audit.riskscoring.masterdata.service.AuditObjectInspectionHistoryService;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/** "Lich su KT" cua Doi tuong kiem toan - nhung trong man hinh Doi tuong kiem toan, dung chung
 * quyen AUDIT.RISK_SCORING.* voi man hinh cha (xem AuditObjectInspectionHistoryService). */
@RestController
@RequestMapping("/api/audit/risk-scoring/master-data/audit-object-inspection-history")
public class AuditObjectInspectionHistoryController {

    private final AuditObjectInspectionHistoryService service;

    public AuditObjectInspectionHistoryController(AuditObjectInspectionHistoryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING.VIEW')")
    public ApiResponse<List<AuditObjectInspectionHistoryResponse>> listByUnit(@RequestParam("auditObjectUnitId") UUID auditObjectUnitId) {
        return ApiResponse.ok(service.listByUnit(auditObjectUnitId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING.CREATE')")
    public ApiResponse<AuditObjectInspectionHistoryResponse> create(@Valid @RequestBody AuditObjectInspectionHistoryRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING.EDIT')")
    public ApiResponse<AuditObjectInspectionHistoryResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditObjectInspectionHistoryRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
