package com.govia.audit.tdkp.unitrec;

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

/** Sheet 6 - ZTC_TDKP_KTNB. */
@RestController
@RequestMapping("/api/audit/tdkp/unit-recommendation")
public class AuditTdkpUnitRecommendationController {

    private final AuditTdkpUnitRecommendationService service;

    public AuditTdkpUnitRecommendationController(AuditTdkpUnitRecommendationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_KTNB.VIEW')")
    public ApiResponse<List<AuditTdkpUnitRecommendationDto.Response>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_KTNB.CREATE')")
    public ApiResponse<AuditTdkpUnitRecommendationDto.Response> create(@Valid @RequestBody AuditTdkpUnitRecommendationDto.Request request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_KTNB.EDIT')")
    public ApiResponse<AuditTdkpUnitRecommendationDto.Response> update(@PathVariable UUID id, @Valid @RequestBody AuditTdkpUnitRecommendationDto.Request request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_KTNB.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_KTNB.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return TdkpSupport.xlsx("tdkp_kien_nghi_don_vi.xlsx", service.exportExcel());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_KTNB.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(file));
    }
}
