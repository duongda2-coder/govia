package com.govia.audit.planengagement.ttss.controller;

import com.govia.audit.planengagement.ttss.dto.AuditTtssApproveRecommendationsRequest;
import com.govia.audit.planengagement.ttss.dto.AuditTtssLinkRecommendationRequest;
import com.govia.audit.planengagement.ttss.dto.AuditTtssRecordResponse;
import com.govia.audit.planengagement.ttss.service.AuditTtssService;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** "Quản lý TTSS & Kiến nghị" (Khối C, man hinh "C" trong Quan ly cong viec). */
@RestController
@RequestMapping("/api/audit/plan/engagement/{engagementId}/ttss")
public class AuditTtssController {

    private final AuditTtssService service;

    public AuditTtssController(AuditTtssService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.VIEW')")
    public ApiResponse<List<AuditTtssRecordResponse>> list(@PathVariable UUID engagementId) {
        return ApiResponse.ok(service.list(engagementId));
    }

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.VIEW')")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable UUID engagementId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau_upload_ttss.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.downloadTemplate(engagementId));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.EDIT')")
    public ApiResponse<List<AuditTtssRecordResponse>> upload(@PathVariable UUID engagementId, @RequestParam("file") MultipartFile file,
                                                              @RequestParam(required = false) String note,
                                                              @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.upload(engagementId, file, note, principal));
    }

    @PostMapping("/link-recommendation")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.EDIT')")
    public ApiResponse<List<AuditTtssRecordResponse>> linkRecommendation(@PathVariable UUID engagementId,
                                                                          @Valid @RequestBody AuditTtssLinkRecommendationRequest request) {
        return ApiResponse.ok(service.linkRecommendation(engagementId, request));
    }

    @PostMapping("/approve-recommendations")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.APPROVE')")
    public ApiResponse<List<UUID>> approveRecommendations(@PathVariable UUID engagementId,
                                                           @Valid @RequestBody AuditTtssApproveRecommendationsRequest request,
                                                           @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.approveRecommendations(engagementId, request, principal));
    }
}
