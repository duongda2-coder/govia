package com.govia.audit.khkt.scale.controller;

import com.govia.audit.khkt.scale.dto.AuditKhktScaleRequest;
import com.govia.audit.khkt.scale.dto.AuditKhktScaleResponse;
import com.govia.audit.khkt.scale.service.AuditKhktScaleService;
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

/** Man hinh "Danh muc Quy mô tín dụng và huy động vốn của chi nhánh" (sheet ZTC_KHKT_QM, xem AuditKhktScaleService). */
@RestController
@RequestMapping("/api/audit/plan/master-data/khkt-scale")
public class AuditKhktScaleController {

    private final AuditKhktScaleService service;

    public AuditKhktScaleController(AuditKhktScaleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_SCALE.VIEW')")
    public ApiResponse<List<AuditKhktScaleResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_SCALE.CREATE')")
    public ApiResponse<AuditKhktScaleResponse> create(@Valid @RequestBody AuditKhktScaleRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_SCALE.EDIT')")
    public ApiResponse<AuditKhktScaleResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditKhktScaleRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_SCALE.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_SCALE.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_khkt_scale.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_SCALE.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_khkt_scale.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_SCALE.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(file));
    }
}
