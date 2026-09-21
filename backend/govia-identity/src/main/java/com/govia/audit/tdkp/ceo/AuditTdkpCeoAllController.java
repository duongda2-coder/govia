package com.govia.audit.tdkp.ceo;

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

/** Sheet 2 - ZTC_TDKP_CEO_ALL: kiến nghị đối với HĐTV/TGĐ do các Phòng nghiệp vụ của KTNB cập nhật. */
@RestController
@RequestMapping("/api/audit/tdkp/ceo-all")
public class AuditTdkpCeoAllController {

    private final AuditTdkpCeoRecommendationService service;

    public AuditTdkpCeoAllController(AuditTdkpCeoRecommendationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_ALL.VIEW')")
    public ApiResponse<List<AuditTdkpCeoRecommendationDto.Response>> list() {
        return ApiResponse.ok(service.list(TdkpCeoScope.ALL));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_ALL.CREATE')")
    public ApiResponse<AuditTdkpCeoRecommendationDto.Response> create(@Valid @RequestBody AuditTdkpCeoRecommendationDto.Request request) {
        return ApiResponse.ok(service.create(TdkpCeoScope.ALL, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_ALL.EDIT')")
    public ApiResponse<AuditTdkpCeoRecommendationDto.Response> update(@PathVariable UUID id, @Valid @RequestBody AuditTdkpCeoRecommendationDto.Request request) {
        return ApiResponse.ok(service.update(TdkpCeoScope.ALL, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_ALL.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(TdkpCeoScope.ALL, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_ALL.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return TdkpSupport.xlsx("tdkp_ceo_all.xlsx", service.exportExcel(TdkpCeoScope.ALL));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_ALL.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(TdkpCeoScope.ALL, file));
    }
}
