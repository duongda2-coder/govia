package com.govia.audit.planengagement.controller;

import com.govia.audit.planengagement.dto.AuditEngagementRelatedUnitRequest;
import com.govia.audit.planengagement.dto.AuditEngagementRelatedUnitResponse;
import com.govia.audit.planengagement.dto.AuditEngagementRequest;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import com.govia.audit.planengagement.dto.AuditObjectUnitOption;
import com.govia.audit.planengagement.dto.EmployeeOption;
import com.govia.audit.planengagement.service.AuditEngagementService;
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

/** Man hinh "Khoi tao va quan ly cuoc kiem toan" (sheet "khoi tao" cua Tao CKT.xlsx). */
@RestController
@RequestMapping("/api/audit/plan/engagement")
public class AuditEngagementController {

    private final AuditEngagementService service;

    public AuditEngagementController(AuditEngagementService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.VIEW')")
    public ApiResponse<List<AuditEngagementResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.VIEW')")
    public ApiResponse<AuditEngagementResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.CREATE')")
    public ApiResponse<AuditEngagementResponse> create(@Valid @RequestBody AuditEngagementRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.EDIT')")
    public ApiResponse<AuditEngagementResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditEngagementRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/lookups/audit-object-units")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.VIEW')")
    public ApiResponse<List<AuditObjectUnitOption>> lookupAuditObjectUnits() {
        return ApiResponse.ok(service.listAuditObjectUnitOptions());
    }

    @GetMapping("/lookups/employees")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.VIEW')")
    public ApiResponse<List<EmployeeOption>> lookupEmployees() {
        return ApiResponse.ok(service.listEmployeeOptions());
    }

    @GetMapping("/{id}/related-units")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.VIEW')")
    public ApiResponse<List<AuditEngagementRelatedUnitResponse>> listRelatedUnits(@PathVariable UUID id) {
        return ApiResponse.ok(service.listRelatedUnits(id));
    }

    @PostMapping("/{id}/related-units")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.EDIT')")
    public ApiResponse<AuditEngagementRelatedUnitResponse> addRelatedUnit(@PathVariable UUID id, @Valid @RequestBody AuditEngagementRelatedUnitRequest request) {
        return ApiResponse.ok(service.addRelatedUnit(id, request));
    }

    @DeleteMapping("/{id}/related-units/{relatedUnitId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.EDIT')")
    public ApiResponse<Void> deleteRelatedUnit(@PathVariable UUID id, @PathVariable UUID relatedUnitId) {
        service.deleteRelatedUnit(id, relatedUnitId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_engagement.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_engagement.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(file));
    }
}
