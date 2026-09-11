package com.govia.audit.planengagement.processengagement.controller;

import com.govia.audit.planengagement.dto.AuditWorkReportFileResponse;
import com.govia.audit.planengagement.processengagement.service.AuditProcessEngagementReportFileService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** "2. File báo cáo khác" (man hinh "QL CKT quy trinh") - download dung thang endpoint chung
 * {@code GET /api/attachments/{id}/download}. */
@RestController
@RequestMapping("/api/audit/plan/engagement-process/{processEngagementId}/report-files")
public class AuditProcessEngagementReportFileController {

    private final AuditProcessEngagementReportFileService service;

    public AuditProcessEngagementReportFileController(AuditProcessEngagementReportFileService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.VIEW')")
    public ApiResponse<List<AuditWorkReportFileResponse>> list(@PathVariable UUID processEngagementId) {
        return ApiResponse.ok(service.list(processEngagementId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.EDIT')")
    public ApiResponse<AuditWorkReportFileResponse> upload(@PathVariable UUID processEngagementId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.upload(processEngagementId, file));
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.PLAN_ENGAGEMENT_PROCESS.EDIT')")
    public ApiResponse<Void> delete(@PathVariable UUID processEngagementId, @PathVariable UUID attachmentId,
                                     @AuthenticationPrincipal CurrentUserPrincipal principal) {
        service.delete(processEngagementId, attachmentId, principal.username());
        return ApiResponse.ok(null);
    }
}
