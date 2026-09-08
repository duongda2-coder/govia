package com.govia.audit.planengagement.recommendation.controller;

import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationRequest;
import com.govia.audit.planengagement.recommendation.dto.AuditRecommendationResponse;
import com.govia.audit.planengagement.recommendation.service.AuditRecommendationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** "3. Thêm kiến nghị" (man hinh "Quản lý TTSS & Kiến nghị"). */
@RestController
@RequestMapping("/api/audit/plan/engagement/{engagementId}/ttss/recommendations")
public class AuditRecommendationController {

    private final AuditRecommendationService service;

    public AuditRecommendationController(AuditRecommendationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.VIEW')")
    public ApiResponse<List<AuditRecommendationResponse>> list(@PathVariable UUID engagementId) {
        return ApiResponse.ok(service.list(engagementId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.EDIT')")
    public ApiResponse<AuditRecommendationResponse> create(@PathVariable UUID engagementId, @Valid @RequestBody AuditRecommendationRequest request) {
        return ApiResponse.ok(service.create(engagementId, request));
    }

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.VIEW')")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable UUID engagementId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau_kien_nghi.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.downloadTemplate(engagementId));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.EDIT')")
    public ApiResponse<List<AuditRecommendationResponse>> upload(@PathVariable UUID engagementId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.upload(engagementId, file));
    }

    @DeleteMapping("/{recommendationId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.TTSS.EDIT')")
    public ApiResponse<Void> delete(@PathVariable UUID engagementId, @PathVariable UUID recommendationId) {
        service.delete(engagementId, recommendationId);
        return ApiResponse.ok(null);
    }
}
