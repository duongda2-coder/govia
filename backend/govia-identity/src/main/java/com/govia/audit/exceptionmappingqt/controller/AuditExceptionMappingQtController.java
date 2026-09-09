package com.govia.audit.exceptionmappingqt.controller;

import com.govia.audit.exceptionmappingqt.dto.AuditExceptionMappingQtRequest;
import com.govia.audit.exceptionmappingqt.dto.AuditExceptionMappingQtResponse;
import com.govia.audit.exceptionmappingqt.service.AuditExceptionMappingQtService;
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

/** Man hinh "Danh muc Mapping ton tai sai sot quy trinh" (sheet ZTC_TTSS_MAP_QT, xem AuditExceptionMappingQtService). */
@RestController
@RequestMapping("/api/audit/plan/master-data-qt/exception-mapping")
public class AuditExceptionMappingQtController {

    private final AuditExceptionMappingQtService service;

    public AuditExceptionMappingQtController(AuditExceptionMappingQtService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_MAPPING_QT.VIEW')")
    public ApiResponse<List<AuditExceptionMappingQtResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_MAPPING_QT.CREATE')")
    public ApiResponse<AuditExceptionMappingQtResponse> create(@Valid @RequestBody AuditExceptionMappingQtRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_MAPPING_QT.EDIT')")
    public ApiResponse<AuditExceptionMappingQtResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditExceptionMappingQtRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_MAPPING_QT.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_MAPPING_QT.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_exception_mapping_qt.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_MAPPING_QT.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_exception_mapping_qt.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_MAPPING_QT.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(file));
    }
}
