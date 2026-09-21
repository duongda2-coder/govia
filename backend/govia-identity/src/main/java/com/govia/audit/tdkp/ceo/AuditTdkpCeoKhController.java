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

/** Sheet 3 - ZTC_TDKP_CEO_KH: kiến nghị đối với HĐTV/TGĐ do nhân sự Phòng Kế hoạch cập nhật (chuyển từ CEO_ALL, cho phép sửa cả cột tự động). */
@RestController
@RequestMapping("/api/audit/tdkp/ceo-kh")
public class AuditTdkpCeoKhController {

    private final AuditTdkpCeoRecommendationService service;

    public AuditTdkpCeoKhController(AuditTdkpCeoRecommendationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_KH.VIEW')")
    public ApiResponse<List<AuditTdkpCeoRecommendationDto.Response>> list() {
        return ApiResponse.ok(service.list(TdkpCeoScope.KH));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_KH.CREATE')")
    public ApiResponse<AuditTdkpCeoRecommendationDto.Response> create(@Valid @RequestBody AuditTdkpCeoRecommendationDto.Request request) {
        return ApiResponse.ok(service.create(TdkpCeoScope.KH, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_KH.EDIT')")
    public ApiResponse<AuditTdkpCeoRecommendationDto.Response> update(@PathVariable UUID id, @Valid @RequestBody AuditTdkpCeoRecommendationDto.Request request) {
        return ApiResponse.ok(service.update(TdkpCeoScope.KH, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_KH.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(TdkpCeoScope.KH, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/transfer-from-all")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_KH.TRANSFER')")
    public ApiResponse<AuditTdkpCeoRecommendationDto.TransferResult> transferFromAll() {
        return ApiResponse.ok(service.transferFromAll());
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_KH.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return TdkpSupport.xlsx("tdkp_ceo_kh.xlsx", service.exportExcel(TdkpCeoScope.KH));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CEO_KH.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(TdkpCeoScope.KH, file));
    }
}
