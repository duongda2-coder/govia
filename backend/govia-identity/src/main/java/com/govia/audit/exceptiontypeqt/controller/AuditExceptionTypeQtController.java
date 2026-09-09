package com.govia.audit.exceptiontypeqt.controller;

import com.govia.audit.exceptiontypeqt.dto.AuditExceptionTypeQtRequest;
import com.govia.audit.exceptiontypeqt.dto.AuditExceptionTypeQtResponse;
import com.govia.audit.exceptiontypeqt.service.AuditExceptionTypeQtService;
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

/** Man hinh "Danh muc Ton tai sai sot quy trinh" (sheet ZTC_TTSS_QT, xem AuditExceptionTypeQtService). */
@RestController
@RequestMapping("/api/audit/plan/master-data-qt/exception-type")
public class AuditExceptionTypeQtController {

    private final AuditExceptionTypeQtService service;

    public AuditExceptionTypeQtController(AuditExceptionTypeQtService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_TYPE_QT.VIEW')")
    public ApiResponse<List<AuditExceptionTypeQtResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_TYPE_QT.CREATE')")
    public ApiResponse<AuditExceptionTypeQtResponse> create(@Valid @RequestBody AuditExceptionTypeQtRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_TYPE_QT.EDIT')")
    public ApiResponse<AuditExceptionTypeQtResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditExceptionTypeQtRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_TYPE_QT.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_TYPE_QT.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_exception_type_qt.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_TYPE_QT.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_exception_type_qt.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EXCEPTION_TYPE_QT.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(file));
    }
}
