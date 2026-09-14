package com.govia.audit.khkt.thang.controller;

import com.govia.audit.khkt.thang.dto.AuditKhktThangRowResponse;
import com.govia.audit.khkt.thang.dto.AuditKhktThangUpdateRequest;
import com.govia.audit.khkt.thang.service.AuditKhktThangService;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** "Khai bao so thang kiem toan trong nam" (sheet ZTC_KHKT_THANG) - xem AuditKhktThangService. */
@RestController
@RequestMapping("/api/audit/plan/khkt-thang")
public class AuditKhktThangController {

    private final AuditKhktThangService service;

    public AuditKhktThangController(AuditKhktThangService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_THANG.VIEW')")
    public ApiResponse<List<AuditKhktThangRowResponse>> list(@RequestParam Integer year) {
        return ApiResponse.ok(service.list(year));
    }

    @PutMapping("/{year}/{auditObjectCode}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_THANG.EDIT')")
    public ApiResponse<AuditKhktThangRowResponse> update(@PathVariable Integer year, @PathVariable String auditObjectCode,
                                                          @Valid @RequestBody AuditKhktThangUpdateRequest request) {
        return ApiResponse.ok(service.update(year, auditObjectCode, request));
    }
}
