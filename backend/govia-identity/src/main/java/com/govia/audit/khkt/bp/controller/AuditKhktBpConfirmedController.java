package com.govia.audit.khkt.bp.controller;

import com.govia.audit.khkt.bp.dto.AuditKhktBpRowResponse;
import com.govia.audit.khkt.bp.service.AuditKhktBpService;
import com.govia.core.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** "De xuat DTKT nam theo phong" - Phan 2 (da xac nhan, CHI DOC), sheet ZTC_KHKT_BP2. Chi duoc
 * ghi qua AuditKhktBpController#confirm - xem AuditKhktBpService. */
@RestController
@RequestMapping("/api/audit/plan/khkt-bp-confirmed")
public class AuditKhktBpConfirmedController {

    private final AuditKhktBpService service;

    public AuditKhktBpConfirmedController(AuditKhktBpService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_BP.VIEW')")
    public ApiResponse<List<AuditKhktBpRowResponse>> list(@RequestParam UUID departmentId, @RequestParam Integer year) {
        return ApiResponse.ok(service.listConfirmed(departmentId, year));
    }

    /** "Trang thai" (BJ) - nut phe duyet/chua phe duyet, KHONG sua duoc du lieu nao khac cua dong
     * da xac nhan (xem AuditKhktBpService#setApprovalStatus). */
    @PatchMapping("/{id}/approval-status")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_BP.EDIT')")
    public ApiResponse<AuditKhktBpRowResponse> setApprovalStatus(@PathVariable UUID id, @RequestParam boolean approved) {
        return ApiResponse.ok(service.setApprovalStatus(id, approved));
    }
}
