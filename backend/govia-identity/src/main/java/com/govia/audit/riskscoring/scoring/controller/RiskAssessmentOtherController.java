package com.govia.audit.riskscoring.scoring.controller;

import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherHeaderRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherHeaderResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherLineRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherLineResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherRowResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskAssessmentOtherHeader;
import com.govia.audit.riskscoring.scoring.service.RiskAssessmentOtherHeaderService;
import com.govia.audit.riskscoring.scoring.service.RiskAssessmentOtherLineService;
import com.govia.core.export.ImportResult;
import com.govia.core.tenant.TenantContext;
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

/**
 * Man hinh "Cham diem rui ro HO, CNTT, Du an, Dich vu thue ngoai..." cua sub-module Cham Diem
 * (sheet ZTC_CDRR_KHAC) - header + line (xem RiskAssessmentOtherHeaderService, RiskAssessmentOtherLineService).
 */
@RestController
@RequestMapping("/api/audit/risk-scoring/scoring/assessment-other")
public class RiskAssessmentOtherController {

    private final RiskAssessmentOtherHeaderService headerService;
    private final RiskAssessmentOtherLineService lineService;

    public RiskAssessmentOtherController(RiskAssessmentOtherHeaderService headerService, RiskAssessmentOtherLineService lineService) {
        this.headerService = headerService;
        this.lineService = lineService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskAssessmentOtherHeaderResponse>> list() {
        return ApiResponse.ok(headerService.list());
    }

    /** Ban "phang" cua list() - 1 dong/1 chi tieu, dung de man hinh danh sach hien theo dung 6 cot
     * cua file Excel export/import (thay vi 1 dong/1 header nhu list()). */
    @GetMapping("/rows")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskAssessmentOtherRowResponse>> rows() {
        return ApiResponse.ok(headerService.listRows());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.CREATE')")
    public ApiResponse<RiskAssessmentOtherHeaderResponse> create(@Valid @RequestBody RiskAssessmentOtherHeaderRequest request) {
        return ApiResponse.ok(headerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EDIT')")
    public ApiResponse<RiskAssessmentOtherHeaderResponse> update(@PathVariable UUID id, @Valid @RequestBody RiskAssessmentOtherHeaderRequest request) {
        return ApiResponse.ok(headerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        headerService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/lines")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskAssessmentOtherLineResponse>> lines(@PathVariable UUID id) {
        RiskAssessmentOtherHeader header = headerService.getOwnedOrThrow(TenantContext.getTenantId(), id);
        return ApiResponse.ok(lineService.listByHeader(header));
    }

    @PutMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EDIT')")
    public ApiResponse<RiskAssessmentOtherLineResponse> updateLine(@PathVariable UUID id, @PathVariable UUID lineId,
                                                                     @RequestBody RiskAssessmentOtherLineRequest request) {
        RiskAssessmentOtherHeader header = headerService.getOwnedOrThrow(TenantContext.getTenantId(), id);
        return ApiResponse.ok(lineService.updateScore(header, lineId, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.DELETE')")
    public ApiResponse<Void> deleteLine(@PathVariable UUID id, @PathVariable UUID lineId) {
        RiskAssessmentOtherHeader header = headerService.getOwnedOrThrow(TenantContext.getTenantId(), id);
        lineService.delete(header, lineId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_assessment_other.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(headerService.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_assessment_other.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(headerService.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(headerService.importFromExcel(file));
    }
}
