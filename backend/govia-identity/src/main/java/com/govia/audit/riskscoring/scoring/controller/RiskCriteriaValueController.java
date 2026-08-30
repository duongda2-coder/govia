package com.govia.audit.riskscoring.scoring.controller;

import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQualitativeValueRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQualitativeValueResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQuantitativeValueRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQuantitativeValueResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQuantitativeWideRowRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQuantitativeWideRowResponse;
import com.govia.audit.riskscoring.scoring.service.RiskCriteriaQualitativeValueService;
import com.govia.audit.riskscoring.scoring.service.RiskCriteriaQuantitativeValueService;
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

/** Man hinh "Ho so rui ro" (sheet ZTC_HSRR) - 2 nut upload doc lap: dinh luong (wide-format, co
 * phan quyen theo user/chi tieu) va dinh tinh (long-format). Xem RiskCriteriaQuantitativeValueService
 * / RiskCriteriaQualitativeValueService. */
@RestController
@RequestMapping("/api/audit/risk-scoring/scoring/hsrr")
public class RiskCriteriaValueController {

    private final RiskCriteriaQuantitativeValueService quantitativeService;
    private final RiskCriteriaQualitativeValueService qualitativeService;

    public RiskCriteriaValueController(RiskCriteriaQuantitativeValueService quantitativeService,
                                        RiskCriteriaQualitativeValueService qualitativeService) {
        this.quantitativeService = quantitativeService;
        this.qualitativeService = qualitativeService;
    }

    @GetMapping("/quantitative")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskCriteriaQuantitativeValueResponse>> listQuantitative(@RequestParam Integer year) {
        return ApiResponse.ok(quantitativeService.list(year));
    }

    @PostMapping("/quantitative")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.CREATE')")
    public ApiResponse<RiskCriteriaQuantitativeValueResponse> createQuantitative(@Valid @RequestBody RiskCriteriaQuantitativeValueRequest request) {
        return ApiResponse.ok(quantitativeService.create(request));
    }

    @PutMapping("/quantitative/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EDIT')")
    public ApiResponse<RiskCriteriaQuantitativeValueResponse> updateQuantitative(@PathVariable UUID id,
                                                                                  @Valid @RequestBody RiskCriteriaQuantitativeValueRequest request) {
        return ApiResponse.ok(quantitativeService.update(id, request));
    }

    @DeleteMapping("/quantitative/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.DELETE')")
    public ApiResponse<Void> deleteQuantitative(@PathVariable UUID id) {
        quantitativeService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/quantitative/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportQuantitativeExcel(@RequestParam Integer year) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_criteria_quantitative_value.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(quantitativeService.exportExcel(year));
    }

    @GetMapping("/quantitative/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportQuantitativeWord(@RequestParam Integer year) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_criteria_quantitative_value.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(quantitativeService.exportWord(year));
    }

    @PostMapping("/quantitative/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.IMPORT')")
    public ApiResponse<ImportResult> importQuantitative(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(quantitativeService.importFromExcel(file));
    }

    /** Ban "wide" cua man hinh dinh luong (1 dong = 1 chi nhanh/nam, tung chi tieu 1 cot) - dung
     * dinh dang voi sheet DL_Nhaptructiep / mau DL_HSRR_Upload. */
    @GetMapping("/quantitative/wide")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskCriteriaQuantitativeWideRowResponse>> listQuantitativeWide(@RequestParam Integer year) {
        return ApiResponse.ok(quantitativeService.listWide(year));
    }

    @PutMapping("/quantitative/wide")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EDIT')")
    public ApiResponse<RiskCriteriaQuantitativeWideRowResponse> saveQuantitativeWideRow(
            @Valid @RequestBody RiskCriteriaQuantitativeWideRowRequest request) {
        return ApiResponse.ok(quantitativeService.saveWideRow(request));
    }

    @DeleteMapping("/quantitative/wide")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.DELETE')")
    public ApiResponse<Void> deleteQuantitativeWideRow(@RequestParam String branchCode, @RequestParam Integer year) {
        quantitativeService.deleteWideRow(branchCode, year);
        return ApiResponse.ok(null);
    }

    @GetMapping("/qualitative")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskCriteriaQualitativeValueResponse>> listQualitative(@RequestParam Integer year) {
        return ApiResponse.ok(qualitativeService.list(year));
    }

    @PostMapping("/qualitative")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.CREATE')")
    public ApiResponse<RiskCriteriaQualitativeValueResponse> createQualitative(@Valid @RequestBody RiskCriteriaQualitativeValueRequest request) {
        return ApiResponse.ok(qualitativeService.create(request));
    }

    @PutMapping("/qualitative/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EDIT')")
    public ApiResponse<RiskCriteriaQualitativeValueResponse> updateQualitative(@PathVariable UUID id,
                                                                                @Valid @RequestBody RiskCriteriaQualitativeValueRequest request) {
        return ApiResponse.ok(qualitativeService.update(id, request));
    }

    @DeleteMapping("/qualitative/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.DELETE')")
    public ApiResponse<Void> deleteQualitative(@PathVariable UUID id) {
        qualitativeService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/qualitative/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportQualitativeExcel(@RequestParam Integer year) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_criteria_qualitative_value.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(qualitativeService.exportExcel(year));
    }

    @GetMapping("/qualitative/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EXPORT')")
    public ResponseEntity<byte[]> exportQualitativeWord(@RequestParam Integer year) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"risk_score_criteria_qualitative_value.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(qualitativeService.exportWord(year));
    }

    @PostMapping("/qualitative/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.IMPORT')")
    public ApiResponse<ImportResult> importQualitative(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(qualitativeService.importFromExcel(file));
    }
}
