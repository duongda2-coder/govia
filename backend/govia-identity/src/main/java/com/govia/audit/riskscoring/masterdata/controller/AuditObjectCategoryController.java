package com.govia.audit.riskscoring.masterdata.controller;

import com.govia.audit.riskscoring.masterdata.dto.AuditObjectCategoryRequest;
import com.govia.audit.riskscoring.masterdata.dto.AuditObjectCategoryResponse;
import com.govia.audit.riskscoring.masterdata.service.AuditObjectCategoryService;
import com.govia.core.export.ImportResult;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

/** Man hinh "Loai doi tuong kiem toan" cua sub-module Master Data CDRR (xem AuditObjectCategoryService). */
@RestController
@RequestMapping("/api/audit/risk-scoring/master-data/audit-object-category")
public class AuditObjectCategoryController {

    private final AuditObjectCategoryService service;

    public AuditObjectCategoryController(AuditObjectCategoryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING.VIEW')")
    public ApiResponse<List<AuditObjectCategoryResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING.CREATE')")
    public ApiResponse<AuditObjectCategoryResponse> create(@Valid @RequestBody AuditObjectCategoryRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING.EDIT')")
    public ApiResponse<AuditObjectCategoryResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditObjectCategoryRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_audit_object_category.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_audit_object_category.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(file));
    }
}
