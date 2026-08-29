package com.govia.audit.riskscoring.scoring.controller;

import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreCombinedRowResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreQualitativeRowResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreQuantitativeRowResponse;
import com.govia.audit.riskscoring.scoring.service.RiskBranchScoreCombinedService;
import com.govia.audit.riskscoring.scoring.service.RiskBranchScoreQualitativeService;
import com.govia.audit.riskscoring.scoring.service.RiskBranchScoreQuantitativeService;
import com.govia.core.web.ApiResponse;
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

    @GetMapping("/qualitative")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskBranchScoreQualitativeRowResponse>> listQualitative(@RequestParam Integer year) {
        return ApiResponse.ok(qualitativeService.list(year));
    }

    @GetMapping("/combined")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskBranchScoreCombinedRowResponse>> listCombined(@RequestParam Integer year) {
        return ApiResponse.ok(combinedService.list(year));
    }
}
