package com.govia.audit.riskscoring.scoring.controller;

import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherExpertRankRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherExpertRankResponse;
import com.govia.audit.riskscoring.scoring.service.RiskAssessmentOtherExpertRankService;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Man hinh "Xep hang rui ro theo y kien chuyen gia cua DTKT khac" (sheet ZTC_XHRR_KHAC_CG) - xem
 * RiskAssessmentOtherExpertRankService. */
@RestController
@RequestMapping("/api/audit/risk-scoring/scoring/assessment-other-expert-rank")
public class RiskAssessmentOtherExpertRankController {

    private final RiskAssessmentOtherExpertRankService service;

    public RiskAssessmentOtherExpertRankController(RiskAssessmentOtherExpertRankService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskAssessmentOtherExpertRankResponse>> list(@RequestParam Integer year) {
        return ApiResponse.ok(service.list(year));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.CREATE')")
    public ApiResponse<List<RiskAssessmentOtherExpertRankResponse>> sync(@RequestParam Integer year) {
        return ApiResponse.ok(service.syncFromSource(year));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EDIT')")
    public ApiResponse<RiskAssessmentOtherExpertRankResponse> update(@PathVariable UUID id,
                                                                       @Valid @RequestBody RiskAssessmentOtherExpertRankRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }
}
