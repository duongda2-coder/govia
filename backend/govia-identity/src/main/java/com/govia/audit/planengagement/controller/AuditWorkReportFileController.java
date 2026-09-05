package com.govia.audit.planengagement.controller;

import com.govia.audit.planengagement.dto.AuditWorkReportFileResponse;
import com.govia.audit.planengagement.service.AuditWorkReportFileService;
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

/** "1. File báo cáo khác" (man hinh "Quản lý công việc") - bọc AttachmentService dùng chung, chỉ
 * thêm cột dẫn xuất "Loại báo cáo". Download dùng thẳng endpoint chung
 * {@code GET /api/attachments/{id}/download}. */
@RestController
@RequestMapping("/api/audit/plan/engagement/{engagementId}/work-management/report-files")
public class AuditWorkReportFileController {

    private final AuditWorkReportFileService service;

    public AuditWorkReportFileController(AuditWorkReportFileService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.WORK_MANAGEMENT.VIEW')")
    public ApiResponse<List<AuditWorkReportFileResponse>> list(@PathVariable UUID engagementId) {
        return ApiResponse.ok(service.list(engagementId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.WORK_MANAGEMENT.EDIT')")
    public ApiResponse<AuditWorkReportFileResponse> upload(@PathVariable UUID engagementId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.upload(engagementId, file));
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.WORK_MANAGEMENT.EDIT')")
    public ApiResponse<Void> delete(@PathVariable UUID engagementId, @PathVariable UUID attachmentId,
                                     @AuthenticationPrincipal CurrentUserPrincipal principal) {
        service.delete(engagementId, attachmentId, principal.username());
        return ApiResponse.ok(null);
    }
}
