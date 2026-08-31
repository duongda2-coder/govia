package com.govia.audit.riskscoring.scoring.controller;

import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreExpertRankRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreExpertRankResponse;
import com.govia.audit.riskscoring.scoring.service.RiskBranchScoreExpertRankService;
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

/** Man hinh "Xep hang rui ro chi nhanh theo y kien chuyen gia" (sheet ZTC_DGRR_cg) - xem
 * RiskBranchScoreExpertRankService. */
@RestController
@RequestMapping("/api/audit/risk-scoring/scoring/branch-score-expert-rank")
public class RiskBranchScoreExpertRankController {

    private final RiskBranchScoreExpertRankService service;

    public RiskBranchScoreExpertRankController(RiskBranchScoreExpertRankService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskBranchScoreExpertRankResponse>> list(@RequestParam Integer year) {
        return ApiResponse.ok(service.list(year));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.CREATE')")
    public ApiResponse<List<RiskBranchScoreExpertRankResponse>> sync(@RequestParam Integer year) {
        return ApiResponse.ok(service.syncFromSource(year));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.EDIT')")
    public ApiResponse<RiskBranchScoreExpertRankResponse> update(@PathVariable UUID id,
                                                                   @Valid @RequestBody RiskBranchScoreExpertRankRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }
}
