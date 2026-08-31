package com.govia.audit.finding.controller;

import com.govia.audit.finding.dto.AuditFindingRequest;
import com.govia.audit.finding.dto.AuditFindingResponse;
import com.govia.audit.finding.service.AuditFindingService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Man hinh "Phat hien kiem toan" (xem AuditFindingService). */
@RestController
@RequestMapping("/api/audit/findings")
public class AuditFindingController {

    private final AuditFindingService service;

    public AuditFindingController(AuditFindingService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.FINDING.VIEW')")
    public ApiResponse<List<AuditFindingResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.FINDING.CREATE')")
    public ApiResponse<AuditFindingResponse> create(@Valid @RequestBody AuditFindingRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.FINDING.EDIT')")
    public ApiResponse<AuditFindingResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditFindingRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.FINDING.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
