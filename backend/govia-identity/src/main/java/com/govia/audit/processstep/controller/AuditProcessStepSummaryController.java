package com.govia.audit.processstep.controller;

import com.govia.audit.processstep.dto.AuditProcessStepSummaryRequest;
import com.govia.audit.processstep.dto.AuditProcessStepSummaryResponse;
import com.govia.audit.processstep.service.AuditProcessStepSummaryService;
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

/** Man hinh "Danh muc Buoc quy trinh tong hop" (sheet ZTB_BQT_TH, xem AuditProcessStepSummaryService). */
@RestController
@RequestMapping("/api/audit/plan/master-data/process-step-summary")
public class AuditProcessStepSummaryController {

    private final AuditProcessStepSummaryService service;

    public AuditProcessStepSummaryController(AuditProcessStepSummaryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.PROCESS_STEP_SUMMARY.VIEW')")
    public ApiResponse<List<AuditProcessStepSummaryResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.PROCESS_STEP_SUMMARY.CREATE')")
    public ApiResponse<AuditProcessStepSummaryResponse> create(@Valid @RequestBody AuditProcessStepSummaryRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PROCESS_STEP_SUMMARY.EDIT')")
    public ApiResponse<AuditProcessStepSummaryResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditProcessStepSummaryRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PROCESS_STEP_SUMMARY.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PROCESS_STEP_SUMMARY.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_process_step_summary.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PROCESS_STEP_SUMMARY.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_process_step_summary.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PROCESS_STEP_SUMMARY.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(file));
    }
}
