package com.govia.audit.tdkp.branch;

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

/** Sheet 4 - ZTC_TDKP_CN: theo dõi kiến nghị của KTNB đối với Chi nhánh. */
@RestController
@RequestMapping("/api/audit/tdkp/branch")
public class AuditTdkpBranchController {

    private final AuditTdkpBranchService service;

    public AuditTdkpBranchController(AuditTdkpBranchService service) {
        this.service = service;
    }

    // ---- bang theo doi kien nghi tong hop ----

    @GetMapping("/recommendations")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.VIEW')")
    public ApiResponse<List<AuditTdkpBranchDto.RecommendationResponse>> listRecommendations() {
        return ApiResponse.ok(service.listRecommendations());
    }

    @PostMapping("/recommendations")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.CREATE')")
    public ApiResponse<AuditTdkpBranchDto.RecommendationResponse> createRecommendation(@Valid @RequestBody AuditTdkpBranchDto.RecommendationRequest request) {
        return ApiResponse.ok(service.createRecommendation(request));
    }

    @PutMapping("/recommendations/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.EDIT')")
    public ApiResponse<AuditTdkpBranchDto.RecommendationResponse> updateRecommendation(@PathVariable UUID id,
                                                                                      @Valid @RequestBody AuditTdkpBranchDto.RecommendationRequest request) {
        return ApiResponse.ok(service.updateRecommendation(id, request));
    }

    @DeleteMapping("/recommendations/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.DELETE')")
    public ApiResponse<Void> deleteRecommendation(@PathVariable UUID id) {
        service.deleteRecommendation(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.TRANSFER')")
    public ApiResponse<AuditTdkpBranchDto.TransferResult> transfer(@RequestParam(required = false) Integer year) {
        return ApiResponse.ok(service.transferFromExecution(year));
    }

    @GetMapping("/recommendations/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.EXPORT')")
    public ResponseEntity<byte[]> exportRecommendations() {
        return TdkpSupport.xlsx("tdkp_cn_kien_nghi.xlsx", service.exportRecommendations());
    }

    @PostMapping("/recommendations/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.IMPORT')")
    public ApiResponse<ImportResult> importRecommendations(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importRecommendations(file));
    }

    // ---- bang theo doi chi tiet sai sot ----

    @GetMapping("/defects")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.VIEW')")
    public ApiResponse<List<AuditTdkpBranchDto.DefectResponse>> listDefects(@RequestParam(required = false) UUID recommendationId) {
        return ApiResponse.ok(service.listDefects(recommendationId));
    }

    @PostMapping("/defects")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.CREATE')")
    public ApiResponse<AuditTdkpBranchDto.DefectResponse> createDefect(@Valid @RequestBody AuditTdkpBranchDto.DefectRequest request) {
        return ApiResponse.ok(service.createDefect(request));
    }

    @PutMapping("/defects/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.EDIT')")
    public ApiResponse<AuditTdkpBranchDto.DefectResponse> updateDefect(@PathVariable UUID id, @Valid @RequestBody AuditTdkpBranchDto.DefectRequest request) {
        return ApiResponse.ok(service.updateDefect(id, request));
    }

    @DeleteMapping("/defects/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.DELETE')")
    public ApiResponse<Void> deleteDefect(@PathVariable UUID id) {
        service.deleteDefect(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/defects/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.EXPORT')")
    public ResponseEntity<byte[]> exportDefects() {
        return TdkpSupport.xlsx("tdkp_cn_sai_sot.xlsx", service.exportDefects());
    }

    @GetMapping("/recommendations/{id}/staff-options")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TDKP_CN.VIEW')")
    public ApiResponse<List<String>> staffOptions(@PathVariable UUID id) {
        return ApiResponse.ok(service.staffOptions(id));
    }
}
