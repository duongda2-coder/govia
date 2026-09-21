package com.govia.audit.tdkp.assignment;

import com.govia.audit.tdkp.common.TdkpSupport;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

/** Màn hình "Phân công Theo dõi khắc phục" (ZTC_TDKP_PC). */
@RestController
@RequestMapping("/api/audit/tdkp/assignment")
public class AuditTdkpAssignmentController {

    private final AuditTdkpAssignmentService service;

    public AuditTdkpAssignmentController(AuditTdkpAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_PC.VIEW')")
    public ApiResponse<List<AuditTdkpAssignmentDto.Response>> list(@RequestParam TdkpAssignmentScope scope) {
        return ApiResponse.ok(service.list(scope));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_PC.CREATE')")
    public ApiResponse<AuditTdkpAssignmentDto.Response> create(@Valid @RequestBody AuditTdkpAssignmentDto.Request request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_PC.EDIT')")
    public ApiResponse<AuditTdkpAssignmentDto.Response> update(@PathVariable UUID id, @Valid @RequestBody AuditTdkpAssignmentDto.Request request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_PC.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_PC.EXPORT')")
    public ResponseEntity<byte[]> exportExcel(@RequestParam TdkpAssignmentScope scope) {
        return TdkpSupport.xlsx("tdkp_phan_cong_" + scope.name().toLowerCase() + ".xlsx", service.exportExcel(scope));
    }
}
