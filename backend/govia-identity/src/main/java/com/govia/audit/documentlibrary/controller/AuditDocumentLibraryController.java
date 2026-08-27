package com.govia.audit.documentlibrary.controller;

import com.govia.audit.documentlibrary.dto.AuditDocumentLibraryRequest;
import com.govia.audit.documentlibrary.dto.AuditDocumentLibraryResponse;
import com.govia.audit.documentlibrary.service.AuditDocumentLibraryService;
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

/** Man hinh "Danh muc Thu vien tai lieu" (sheet ZTC_TVTL, xem AuditDocumentLibraryService). */
@RestController
@RequestMapping("/api/audit/master-data/document-library")
public class AuditDocumentLibraryController {

    private final AuditDocumentLibraryService service;

    public AuditDocumentLibraryController(AuditDocumentLibraryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.DOCUMENT_LIBRARY.VIEW')")
    public ApiResponse<List<AuditDocumentLibraryResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.DOCUMENT_LIBRARY.CREATE')")
    public ApiResponse<AuditDocumentLibraryResponse> create(@Valid @RequestBody AuditDocumentLibraryRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DOCUMENT_LIBRARY.EDIT')")
    public ApiResponse<AuditDocumentLibraryResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditDocumentLibraryRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DOCUMENT_LIBRARY.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DOCUMENT_LIBRARY.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_document_library.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DOCUMENT_LIBRARY.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_document_library.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.DOCUMENT_LIBRARY.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(file));
    }
}
