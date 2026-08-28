package com.govia.audit.riskscoring.scoring.controller;

import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherRankingResponse;
import com.govia.audit.riskscoring.scoring.service.RiskAssessmentOtherRankingService;
import com.govia.core.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Man hinh "Bang xep hang cham diem rui ro HO, CNTT, Du an, Dich vu thue ngoai..." (sheet
 * ZTC_BXHRR_KHAC) - CHI XEM, khong co them/sua/xoa (xem RiskAssessmentOtherRankingService). */
@RestController
@RequestMapping("/api/audit/risk-scoring/scoring/assessment-other-ranking")
public class RiskAssessmentOtherRankingController {

    private final RiskAssessmentOtherRankingService service;

    public RiskAssessmentOtherRankingController(RiskAssessmentOtherRankingService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskAssessmentOtherRankingResponse>> list(@RequestParam Integer year) {
        return ApiResponse.ok(service.listByYear(year));
    }
}
