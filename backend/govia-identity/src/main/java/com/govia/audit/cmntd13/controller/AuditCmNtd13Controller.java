package com.govia.audit.cmntd13.controller;

import com.govia.audit.cmntd13.dto.AuditCmNtd13Request;
import com.govia.audit.cmntd13.dto.AuditCmNtd13Response;
import com.govia.audit.cmntd13.service.AuditCmNtd13Service;
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

/** Man hinh "Kết quả kiểm toán đơn vị chấp nhận thẻ (07D)" (sheet ZTC_CM_NTD13, xem AuditCmNtd13Service). */
@RestController
@RequestMapping("/api/audit/plan/execution/cm-ntd13")
public class AuditCmNtd13Controller {

    private final AuditCmNtd13Service service;

    public AuditCmNtd13Controller(AuditCmNtd13Service service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.CM_NTD13.VIEW')")
    public ApiResponse<List<AuditCmNtd13Response>> list(@RequestParam UUID engagementId) {
        return ApiResponse.ok(service.list(engagementId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.CM_NTD13.CREATE')")
    public ApiResponse<AuditCmNtd13Response> create(@Valid @RequestBody AuditCmNtd13Request request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.CM_NTD13.EDIT')")
    public ApiResponse<AuditCmNtd13Response> update(@PathVariable UUID id, @Valid @RequestBody AuditCmNtd13Request request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.CM_NTD13.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.CM_NTD13.EXPORT')")
    public ResponseEntity<byte[]> exportExcel(@RequestParam UUID engagementId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_cm_ntd13.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel(engagementId));
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.CM_NTD13.EXPORT')")
    public ResponseEntity<byte[]> exportWord(@RequestParam UUID engagementId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_cm_ntd13.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord(engagementId));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.CM_NTD13.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam UUID engagementId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(engagementId, file));
    }
}
