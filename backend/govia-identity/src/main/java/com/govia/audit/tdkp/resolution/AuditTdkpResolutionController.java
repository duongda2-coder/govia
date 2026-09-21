package com.govia.audit.tdkp.resolution;

import com.govia.audit.tdkp.common.TdkpSupport;
import com.govia.core.export.ImportResult;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** Sheet 5 - ZTC_TDKP_NQ. */
@RestController
@RequestMapping("/api/audit/tdkp/resolution")
public class AuditTdkpResolutionController {

    private final AuditTdkpResolutionService service;

    public AuditTdkpResolutionController(AuditTdkpResolutionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_NQ.VIEW')")
    public ApiResponse<List<AuditTdkpResolutionDto.Response>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_NQ.CREATE')")
    public ApiResponse<AuditTdkpResolutionDto.Response> create(@Valid @RequestBody AuditTdkpResolutionDto.Request request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_NQ.EDIT')")
    public ApiResponse<AuditTdkpResolutionDto.Response> update(@PathVariable UUID id, @Valid @RequestBody AuditTdkpResolutionDto.Request request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_NQ.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_NQ.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return TdkpSupport.xlsx("tdkp_nghi_quyet.xlsx", service.exportExcel());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_NQ.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(file));
    }
}
