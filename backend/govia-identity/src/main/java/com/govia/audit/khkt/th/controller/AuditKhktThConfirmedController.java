package com.govia.audit.khkt.th.controller;

import com.govia.audit.khkt.th.dto.AuditKhktThRowResponse;
import com.govia.audit.khkt.th.service.AuditKhktThService;
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

/** "Danh sach DTKT nam cua Phong ke hoach" - Phan 2 (da xac nhan, CHI DOC = "TH2"), sheet
 * ZTC_KHKT_TH2. Chi duoc ghi qua AuditKhktThController#confirm - xem AuditKhktThService. */
@RestController
@RequestMapping("/api/audit/plan/khkt-th-confirmed")
public class AuditKhktThConfirmedController {

    private final AuditKhktThService service;

    public AuditKhktThConfirmedController(AuditKhktThService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_TH.VIEW')")
    public ApiResponse<List<AuditKhktThRowResponse>> list(@RequestParam Integer year) {
        return ApiResponse.ok(service.listConfirmed(year));
    }

    /** "Trang thai" (AR) - nut phe duyet/chua phe duyet (xem AuditKhktThService#setApprovalStatus). */
    @PatchMapping("/{id}/approval-status")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_TH.EDIT')")
    public ApiResponse<AuditKhktThRowResponse> setApprovalStatus(@PathVariable UUID id, @RequestParam boolean approved) {
        return ApiResponse.ok(service.setApprovalStatus(id, approved));
    }
}
