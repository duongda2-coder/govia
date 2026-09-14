package com.govia.audit.khkt.dtkhfile.controller;

import com.govia.audit.khkt.dtkhfile.dto.AuditKhktDtkhFileResponse;
import com.govia.audit.khkt.dtkhfile.dto.AuditKhktDtkhFileUpdateRequest;
import com.govia.audit.khkt.dtkhfile.service.AuditKhktDtkhFileService;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
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

/** "Báo cáo tham khảo từ các bộ phận cho PKH lập dự thảo" (sheet ZTC_DTKH_FILE) - xem
 * AuditKhktDtkhFileService. */
@RestController
@RequestMapping("/api/audit/plan/khkt-dtkh-file")
public class AuditKhktDtkhFileController {

    private final AuditKhktDtkhFileService service;

    public AuditKhktDtkhFileController(AuditKhktDtkhFileService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_DTKH_FILE.VIEW')")
    public ApiResponse<List<AuditKhktDtkhFileResponse>> list(@RequestParam Integer year) {
        return ApiResponse.ok(service.list(year));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_DTKH_FILE.CREATE')")
    public ApiResponse<AuditKhktDtkhFileResponse> create(@RequestParam Integer year, @RequestParam UUID departmentId,
                                                          @RequestParam(required = false) UUID versionId,
                                                          @RequestParam(required = false) String note,
                                                          @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.create(year, departmentId, versionId, note, file));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_DTKH_FILE.EDIT')")
    public ApiResponse<AuditKhktDtkhFileResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditKhktDtkhFileUpdateRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.KHKT_DTKH_FILE.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
