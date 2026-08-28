package com.govia.audit.riskscoring.scoring.controller;

import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQualitativeValueResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQuantitativeValueResponse;
import com.govia.audit.riskscoring.scoring.service.RiskCriteriaQualitativeValueService;
import com.govia.audit.riskscoring.scoring.service.RiskCriteriaQuantitativeValueService;
import com.govia.core.export.ImportResult;
import com.govia.core.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @PostMapping("/quantitative/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.IMPORT')")
    public ApiResponse<ImportResult> importQuantitative(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(quantitativeService.importFromExcel(file));
    }

    @GetMapping("/qualitative")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskCriteriaQualitativeValueResponse>> listQualitative(@RequestParam Integer year) {
        return ApiResponse.ok(qualitativeService.list(year));
    }

    @PostMapping("/qualitative/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.IMPORT')")
    public ApiResponse<ImportResult> importQualitative(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(qualitativeService.importFromExcel(file));
    }
}
