package com.govia.audit.phbc.report;

import com.govia.audit.phbc.common.PhbcSupport;
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

/** Man hinh "Phat hanh bao cao" (file ztc_phbc.xlsx). */
@RestController
@RequestMapping("/api/audit/phbc")
public class AuditReportIssuanceController {

    private final AuditReportIssuanceService service;

    public AuditReportIssuanceController(AuditReportIssuanceService service) {
        this.service = service;
    }

    // ---- 1. bao cao phat hanh ----

    @GetMapping("/reports")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.VIEW')")
    public ApiResponse<List<AuditReportIssuanceDto.ReportResponse>> listReports() {
        return ApiResponse.ok(service.listReports());
    }

    @PostMapping("/reports")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.CREATE')")
    public ApiResponse<AuditReportIssuanceDto.ReportResponse> createReport(@Valid @RequestBody AuditReportIssuanceDto.ReportRequest request) {
        return ApiResponse.ok(service.createReport(request));
    }

    @PutMapping("/reports/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.EDIT')")
    public ApiResponse<AuditReportIssuanceDto.ReportResponse> updateReport(@PathVariable UUID id, @Valid @RequestBody AuditReportIssuanceDto.ReportRequest request) {
        return ApiResponse.ok(service.updateReport(id, request));
    }

    @DeleteMapping("/reports/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.DELETE')")
    public ApiResponse<Void> deleteReport(@PathVariable UUID id) {
        service.deleteReport(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/reports/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.EXPORT')")
    public ResponseEntity<byte[]> exportReports() {
        return PhbcSupport.xlsx("phbc_bao_cao.xlsx", service.exportReports());
    }

    @PostMapping("/reports/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.IMPORT')")
    public ApiResponse<ImportResult> importReports(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importReports(file));
    }

    // ---- 2. kien nghi trong bao cao ----

    @GetMapping("/recommendations")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.VIEW')")
    public ApiResponse<List<AuditReportIssuanceDto.RecommendationResponse>> listRecommendations(@RequestParam(required = false) UUID reportIssuanceId) {
        return ApiResponse.ok(service.listRecommendations(reportIssuanceId));
    }

    @PostMapping("/recommendations")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.CREATE')")
    public ApiResponse<AuditReportIssuanceDto.RecommendationResponse> createRecommendation(@Valid @RequestBody AuditReportIssuanceDto.RecommendationRequest request) {
        return ApiResponse.ok(service.createRecommendation(request));
    }

    @PutMapping("/recommendations/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.EDIT')")
    public ApiResponse<AuditReportIssuanceDto.RecommendationResponse> updateRecommendation(@PathVariable UUID id,
                                                                                            @Valid @RequestBody AuditReportIssuanceDto.RecommendationRequest request) {
        return ApiResponse.ok(service.updateRecommendation(id, request));
    }

    @DeleteMapping("/recommendations/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.DELETE')")
    public ApiResponse<Void> deleteRecommendation(@PathVariable UUID id) {
        service.deleteRecommendation(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/recommendations/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.EXPORT')")
    public ResponseEntity<byte[]> exportRecommendations() {
        return PhbcSupport.xlsx("phbc_kien_nghi.xlsx", service.exportRecommendations());
    }

    @PostMapping("/recommendations/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.IMPORT')")
    public ApiResponse<ImportResult> importRecommendations(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importRecommendations(file));
    }
}
