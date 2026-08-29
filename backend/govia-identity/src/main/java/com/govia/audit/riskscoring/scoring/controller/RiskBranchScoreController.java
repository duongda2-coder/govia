package com.govia.audit.riskscoring.scoring.controller;

import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreCombinedRowResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreQualitativeRowResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreQuantitativeRowResponse;
import com.govia.audit.riskscoring.scoring.service.RiskBranchScoreCombinedService;
import com.govia.audit.riskscoring.scoring.service.RiskBranchScoreQualitativeService;
import com.govia.audit.riskscoring.scoring.service.RiskBranchScoreQuantitativeService;
import com.govia.core.web.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** "Ket qua cham diem rui ro theo chi nhanh" (sheet CT_Diem_DL/DT/All) - CHI XEM, khong co
 * them/sua/xoa (xem RiskBranchScoreQuantitativeService va cac service lien quan). */
@RestController
@RequestMapping("/api/audit/risk-scoring/scoring/branch-score")
public class RiskBranchScoreController {

    private final RiskBranchScoreQuantitativeService quantitativeService;
    private final RiskBranchScoreQualitativeService qualitativeService;
    private final RiskBranchScoreCombinedService combinedService;

    public RiskBranchScoreController(RiskBranchScoreQuantitativeService quantitativeService,
                                      RiskBranchScoreQualitativeService qualitativeService,
                                      RiskBranchScoreCombinedService combinedService) {
        this.quantitativeService = quantitativeService;
        this.qualitativeService = qualitativeService;
        this.combinedService = combinedService;
    }

    @GetMapping("/quantitative")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskBranchScoreQuantitativeRowResponse>> listQuantitative(@RequestParam Integer year) {
        return ApiResponse.ok(quantitativeService.list(year));
    }

    @GetMapping("/quantitative/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportQuantitativeExcel(@RequestParam Integer year) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_branch_score_quantitative.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(quantitativeService.exportExcel(year));
    }

    @GetMapping("/quantitative/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportQuantitativeWord(@RequestParam Integer year) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_branch_score_quantitative.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(quantitativeService.exportWord(year));
    }

    @GetMapping("/qualitative")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskBranchScoreQualitativeRowResponse>> listQualitative(@RequestParam Integer year) {
        return ApiResponse.ok(qualitativeService.list(year));
    }

    @GetMapping("/qualitative/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportQualitativeExcel(@RequestParam Integer year) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_branch_score_qualitative.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(qualitativeService.exportExcel(year));
    }

    @GetMapping("/qualitative/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportQualitativeWord(@RequestParam Integer year) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_branch_score_qualitative.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(qualitativeService.exportWord(year));
    }

    @GetMapping("/combined")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskBranchScoreCombinedRowResponse>> listCombined(@RequestParam Integer year) {
        return ApiResponse.ok(combinedService.list(year));
    }

    @GetMapping("/combined/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportCombinedExcel(@RequestParam Integer year) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_branch_score_combined.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(combinedService.exportExcel(year));
    }

    @GetMapping("/combined/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportCombinedWord(@RequestParam Integer year) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_branch_score_combined.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(combinedService.exportWord(year));
    }
}
