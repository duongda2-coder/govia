package com.govia.audit.tools.controller;

import com.govia.audit.finding.dto.AuditFindingResponse;
import com.govia.audit.riskscoring.masterdata.dto.AuditObjectUnitResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreCombinedRowResponse;
import com.govia.audit.tools.dto.EvidenceResponse;
import com.govia.audit.tools.dto.RiskBreakdownResponse;
import com.govia.audit.tools.dto.RiskCriteriaToolResponse;
import com.govia.audit.tools.service.AuditToolsService;
import com.govia.core.web.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Be mat API DUY NHAT ma AI Agent duoc goi (xem docs/audit-tools-contract.md) - 10 endpoint,
 * toan bo GET/read-only, tuong ung 1-1 voi 10 tool trong contract. Khong endpoint nao o day ghi/sua
 * du lieu; agent phai dua vao dung nhung gi cac endpoint nay tra ve, khong duoc tu bia them.
 */
@RestController
@RequestMapping("/api/audit/tools")
public class AuditToolsController {

    private final AuditToolsService service;

    public AuditToolsController(AuditToolsService service) {
        this.service = service;
    }

    @GetMapping("/branch-risk")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<RiskBranchScoreCombinedRowResponse> getBranchRisk(@RequestParam String branchCode, @RequestParam Integer year) {
        return ApiResponse.ok(service.getBranchRisk(branchCode, year));
    }

    @GetMapping("/branch-details")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<AuditObjectUnitResponse> getBranchDetails(@RequestParam String branchCode) {
        return ApiResponse.ok(service.getBranchDetails(branchCode));
    }

    @GetMapping("/risk-breakdown")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<RiskBreakdownResponse> getRiskBreakdown(@RequestParam String branchCode, @RequestParam Integer year) {
        return ApiResponse.ok(service.getRiskBreakdown(branchCode, year));
    }

    @GetMapping("/compare-branches")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskBranchScoreCombinedRowResponse>> compareBranches(@RequestParam List<String> branchCodes,
                                                                                  @RequestParam Integer year) {
        return ApiResponse.ok(service.compareBranches(branchCodes, year));
    }

    @GetMapping("/branches")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<AuditObjectUnitResponse>> listBranches(@RequestParam(required = false) String unitType,
                                                                    @RequestParam(required = false) String search,
                                                                    @RequestParam(required = false) Boolean activeOnly) {
        return ApiResponse.ok(service.listBranches(unitType, search, activeOnly));
    }

    @GetMapping("/risk-history")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskBranchScoreCombinedRowResponse>> getRiskHistory(@RequestParam String branchCode,
                                                                                 @RequestParam(required = false) Integer fromYear,
                                                                                 @RequestParam(required = false) Integer toYear) {
        return ApiResponse.ok(service.getRiskHistory(branchCode, fromYear, toYear));
    }

    @GetMapping("/risk-criteria")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskCriteriaToolResponse>> getRiskCriteria(@RequestParam String kind) {
        return ApiResponse.ok(service.getRiskCriteria(kind));
    }

    @GetMapping("/audit-findings")
    @PreAuthorize("hasAuthority('PERM_AUDIT.FINDING.VIEW')")
    public ApiResponse<List<AuditFindingResponse>> getAuditFindings(@RequestParam(required = false) String branchCode,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                                                     @RequestParam(required = false) String severity) {
        return ApiResponse.ok(service.getAuditFindings(branchCode, fromDate, toDate, severity));
    }

    @GetMapping("/top-risk-branches")
    @PreAuthorize("hasAuthority('PERM_AUDIT.RISK_SCORING_EXEC.VIEW')")
    public ApiResponse<List<RiskBranchScoreCombinedRowResponse>> getTopRiskBranches(@RequestParam Integer year,
                                                                                     @RequestParam(required = false) Integer limit,
                                                                                     @RequestParam(required = false) String unitType) {
        return ApiResponse.ok(service.getTopRiskBranches(year, limit, unitType));
    }

    @GetMapping("/evidence")
    @PreAuthorize("hasAuthority('PERM_AUDIT.FINDING.VIEW')")
    public ApiResponse<List<EvidenceResponse>> getEvidence(@RequestParam UUID findingId) {
        return ApiResponse.ok(service.getEvidence(findingId));
    }
}
