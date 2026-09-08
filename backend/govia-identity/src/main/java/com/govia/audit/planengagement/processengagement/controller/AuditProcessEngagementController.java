package com.govia.audit.planengagement.processengagement.controller;

import com.govia.audit.planengagement.processengagement.dto.AuditProcessEngagementRequest;
import com.govia.audit.planengagement.processengagement.dto.AuditProcessEngagementResponse;
import com.govia.audit.planengagement.processengagement.dto.TeamLeadOption;
import com.govia.audit.planengagement.processengagement.service.AuditProcessEngagementService;
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

/** Man hinh "Tao CKT quy trinh" (sheet "man hinh tao CKT quy trinh" cua "Tao CKT (3).xlsx"). */
@RestController
@RequestMapping("/api/audit/plan/engagement-process")
public class AuditProcessEngagementController {

    private final AuditProcessEngagementService service;

    public AuditProcessEngagementController(AuditProcessEngagementService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.VIEW')")
    public ApiResponse<List<AuditProcessEngagementResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.VIEW')")
    public ApiResponse<AuditProcessEngagementResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.CREATE')")
    public ApiResponse<AuditProcessEngagementResponse> create(@Valid @RequestBody AuditProcessEngagementRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.EDIT')")
    public ApiResponse<AuditProcessEngagementResponse> update(@PathVariable UUID id, @Valid @RequestBody AuditProcessEngagementRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/lookups/team-leads")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.VIEW')")
    public ApiResponse<List<TeamLeadOption>> lookupTeamLeads() {
        return ApiResponse.ok(service.listTeamLeadOptions());
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_process_engagement.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_process_engagement.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(file));
    }
}
