package com.govia.audit.khkt.th.controller;

import com.govia.audit.khkt.th.dto.AuditKhktThCandidateUpdateRequest;
import com.govia.audit.khkt.th.dto.AuditKhktThRowResponse;
import com.govia.audit.khkt.th.service.AuditKhktThService;
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

/** "Danh sach DTKT nam cua Phong ke hoach" - Phan 1 (nhap/sua), sheet ZTC_KHKT_TH. Xem
 * AuditKhktThService va AuditKhktThConfirmedController (Phan 2, chi doc). */
@RestController
@RequestMapping("/api/audit/plan/khkt-th")
public class AuditKhktThController {

    private final AuditKhktThService service;

    public AuditKhktThController(AuditKhktThService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_TH.VIEW')")
    public ApiResponse<List<AuditKhktThRowResponse>> list(@RequestParam Integer year) {
        return ApiResponse.ok(service.list(year));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_TH.CREATE')")
    public ApiResponse<Void> sync(@RequestParam Integer year) {
        service.sync(year);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_TH.EDIT')")
    public ApiResponse<AuditKhktThRowResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditKhktThCandidateUpdateRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_TH.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_TH.CREATE')")
    public ApiResponse<Void> confirm(@RequestParam Integer year) {
        service.confirm(year);
        return ApiResponse.ok(null);
    }
}
