package com.govia.audit.khkt.bp.controller;

import com.govia.audit.khkt.bp.dto.AuditKhktBpCandidateRequest;
import com.govia.audit.khkt.bp.dto.AuditKhktBpCandidateUpdateRequest;
import com.govia.audit.khkt.bp.dto.AuditKhktBpRowResponse;
import com.govia.audit.khkt.bp.service.AuditKhktBpService;
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

/** "De xuat DTKT nam theo phong" - Phan 1 (nhap/sua), sheet ZTC_KHKT_BP. Xem AuditKhktBpService
 * va AuditKhktBpConfirmedController (Phan 2, chi doc). */
@RestController
@RequestMapping("/api/audit/plan/khkt-bp")
public class AuditKhktBpController {

    private final AuditKhktBpService service;

    public AuditKhktBpController(AuditKhktBpService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_BP.VIEW')")
    public ApiResponse<List<AuditKhktBpRowResponse>> list(@RequestParam UUID departmentId, @RequestParam Integer year) {
        return ApiResponse.ok(service.list(departmentId, year));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_BP.CREATE')")
    public ApiResponse<AuditKhktBpRowResponse> create(@Valid @RequestBody AuditKhktBpCandidateRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_BP.EDIT')")
    public ApiResponse<AuditKhktBpRowResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditKhktBpCandidateUpdateRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_BP.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_BP.CREATE')")
    public ApiResponse<Void> confirm(@RequestParam UUID departmentId, @RequestParam Integer year) {
        service.confirm(departmentId, year);
        return ApiResponse.ok(null);
    }
}
