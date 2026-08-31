package com.govia.audit.tools.service;

import com.govia.audit.finding.dto.AuditFindingResponse;
import com.govia.audit.finding.service.AuditFindingService;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.service.MasterDataItemService;
import com.govia.audit.riskscoring.masterdata.dto.AuditObjectUnitResponse;
import com.govia.audit.riskscoring.masterdata.dto.CriteriaQualitativeResponse;
import com.govia.audit.riskscoring.masterdata.dto.CriteriaQuantitativeResponse;
import com.govia.audit.riskscoring.masterdata.service.AuditObjectUnitService;
import com.govia.audit.riskscoring.masterdata.service.CriteriaQualitativeService;
import com.govia.audit.riskscoring.masterdata.service.CriteriaQuantitativeService;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreCombinedRowResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreQualitativeRowResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreQuantitativeRowResponse;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaOtherResponse;
import com.govia.audit.riskscoring.scoring.service.RiskBranchScoreCombinedService;
import com.govia.audit.riskscoring.scoring.service.RiskBranchScoreQualitativeService;
import com.govia.audit.riskscoring.scoring.service.RiskBranchScoreQuantitativeService;
import com.govia.audit.riskscoring.scoring.service.RiskCriteriaOtherService;
import com.govia.audit.tools.dto.EvidenceResponse;
import com.govia.audit.tools.dto.RiskBreakdownResponse;
import com.govia.audit.tools.dto.RiskCriteriaToolResponse;
import com.govia.core.attachment.Attachment;
import com.govia.core.attachment.AttachmentService;
import com.govia.core.web.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lop "Audit Tools" ma AI Agent duoc phep goi (xem docs/audit-tools-contract.md) - toan bo READ-ONLY,
 * moi method CHI compose service da co san (khong dung repository truc tiep, khong viet lai logic
 * tinh diem) roi map sang DTO chuan hoa. Day la bien gioi duy nhat giua agent va du lieu that: agent
 * khong duoc tu bia du lieu, khong co method nao o day duoc phep ghi/sua du lieu.
 */
@Service
public class AuditToolsService {

    private static final int DEFAULT_TOP_LIMIT = 10;

    private final RiskBranchScoreCombinedService combinedService;
    private final RiskBranchScoreQuantitativeService quantitativeScoreService;
    private final RiskBranchScoreQualitativeService qualitativeScoreService;
    private final AuditObjectUnitService auditObjectUnitService;
    private final CriteriaQuantitativeService criteriaQuantitativeService;
    private final CriteriaQualitativeService criteriaQualitativeService;
    private final RiskCriteriaOtherService criteriaOtherService;
    private final AuditFindingService auditFindingService;
    private final AttachmentService attachmentService;
    private final MasterDataItemService masterDataItemService;

    public AuditToolsService(RiskBranchScoreCombinedService combinedService,
                              RiskBranchScoreQuantitativeService quantitativeScoreService,
                              RiskBranchScoreQualitativeService qualitativeScoreService,
                              AuditObjectUnitService auditObjectUnitService,
                              CriteriaQuantitativeService criteriaQuantitativeService,
                              CriteriaQualitativeService criteriaQualitativeService,
                              RiskCriteriaOtherService criteriaOtherService,
                              AuditFindingService auditFindingService,
                              AttachmentService attachmentService,
                              MasterDataItemService masterDataItemService) {
        this.combinedService = combinedService;
        this.quantitativeScoreService = quantitativeScoreService;
        this.qualitativeScoreService = qualitativeScoreService;
        this.auditObjectUnitService = auditObjectUnitService;
        this.criteriaQuantitativeService = criteriaQuantitativeService;
        this.criteriaQualitativeService = criteriaQualitativeService;
        this.criteriaOtherService = criteriaOtherService;
        this.auditFindingService = auditFindingService;
        this.attachmentService = attachmentService;
        this.masterDataItemService = masterDataItemService;
    }

    /** get_branch_risk - null neu chi nhanh chua co diem cham cho nam do (KHONG phai loi). */
    @Transactional(readOnly = true)
    public RiskBranchScoreCombinedRowResponse getBranchRisk(String branchCode, Integer year) {
        return combinedService.list(year).stream()
                .filter(r -> r.branchCode().equalsIgnoreCase(branchCode))
                .findFirst()
                .orElse(null);
    }

    /** get_branch_details - null neu khong tim thay ma chi nhanh. */
    @Transactional(readOnly = true)
    public AuditObjectUnitResponse getBranchDetails(String branchCode) {
        return auditObjectUnitService.list().stream()
                .filter(u -> u.code().equalsIgnoreCase(branchCode))
                .findFirst()
                .orElse(null);
    }

    /** get_risk_breakdown - ghep combined + quantitative + qualitative cho cung 1 chi nhanh/nam. */
    @Transactional(readOnly = true)
    public RiskBreakdownResponse getRiskBreakdown(String branchCode, Integer year) {
        RiskBranchScoreCombinedRowResponse combined = getBranchRisk(branchCode, year);
        if (combined == null) {
            return null;
        }
        Map<String, java.math.BigDecimal> criteriaScores = quantitativeScoreService.list(year).stream()
                .filter(r -> r.branchCode().equalsIgnoreCase(branchCode))
                .findFirst()
                .map(RiskBranchScoreQuantitativeRowResponse::scoresByCriteriaCode)
                .orElse(null);
        Map<String, java.math.BigDecimal> group2Scores = qualitativeScoreService.list(year).stream()
                .filter(r -> r.branchCode().equalsIgnoreCase(branchCode))
                .findFirst()
                .map(RiskBranchScoreQualitativeRowResponse::scoresByGroup2Code)
                .orElse(null);
        return new RiskBreakdownResponse(combined.branchCode(), combined.branchName(), combined.year(),
                combined.totalScore(), combined.rankLabel(), combined.scoresByBusinessLineCode(),
                criteriaScores, group2Scores);
    }

    /** compare_branches - giu nguyen thu tu branchCodes truyen vao, bo qua ma khong co du lieu. */
    @Transactional(readOnly = true)
    public List<RiskBranchScoreCombinedRowResponse> compareBranches(List<String> branchCodes, Integer year) {
        List<RiskBranchScoreCombinedRowResponse> all = combinedService.list(year);
        return branchCodes.stream()
                .map(code -> all.stream().filter(r -> r.branchCode().equalsIgnoreCase(code)).findFirst().orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** list_branches - loc theo unitType/tu khoa tim kiem/trang thai hieu luc, deu tuy chon. */
    @Transactional(readOnly = true)
    public List<AuditObjectUnitResponse> listBranches(String unitType, String search, Boolean activeOnly) {
        String needle = search == null ? null : search.trim().toLowerCase();
        return auditObjectUnitService.list().stream()
                .filter(u -> unitType == null || unitType.equalsIgnoreCase(u.unitType()))
                .filter(u -> !Boolean.TRUE.equals(activeOnly) || u.active())
                .filter(u -> needle == null || u.code().toLowerCase().contains(needle) || u.name().toLowerCase().contains(needle))
                .toList();
    }

    /** get_risk_history - neu khong truyen fromYear/toYear thi lay het khoang nam co trong danh muc Nam. */
    @Transactional(readOnly = true)
    public List<RiskBranchScoreCombinedRowResponse> getRiskHistory(String branchCode, Integer fromYear, Integer toYear) {
        int from = fromYear != null ? fromYear : resolveYearBound(true);
        int to = toYear != null ? toYear : resolveYearBound(false);
        return java.util.stream.IntStream.rangeClosed(Math.min(from, to), Math.max(from, to))
                .mapToObj(year -> getBranchRisk(branchCode, year))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(RiskBranchScoreCombinedRowResponse::year))
                .toList();
    }

    /** get_risk_criteria - kind = "quantitative" | "qualitative" | "other". */
    @Transactional(readOnly = true)
    public List<RiskCriteriaToolResponse> getRiskCriteria(String kind) {
        return switch (kind == null ? "" : kind.toLowerCase()) {
            case "quantitative" -> criteriaQuantitativeService.list().stream().map(this::toToolResponse).toList();
            case "qualitative" -> criteriaQualitativeService.list().stream().map(this::toToolResponse).toList();
            case "other" -> criteriaOtherService.list().stream().map(this::toToolResponse).toList();
            default -> throw new BusinessException("AUDIT_TOOLS_INVALID_KIND",
                    "kind phai la 'quantitative', 'qualitative' hoac 'other', nhan duoc: " + kind);
        };
    }

    /** get_audit_findings - du lieu that tu bang audit_finding, tra mang rong neu chua ai nhap phat hien nao. */
    @Transactional(readOnly = true)
    public List<AuditFindingResponse> getAuditFindings(String branchCode, LocalDate fromDate, LocalDate toDate, String severity) {
        return auditFindingService.search(branchCode, fromDate, toDate, severity);
    }

    /** get_top_risk_branches - sap xep giam dan theo totalScore, mac dinh top 10. */
    @Transactional(readOnly = true)
    public List<RiskBranchScoreCombinedRowResponse> getTopRiskBranches(Integer year, Integer limit, String unitType) {
        int effectiveLimit = limit != null && limit > 0 ? limit : DEFAULT_TOP_LIMIT;
        List<RiskBranchScoreCombinedRowResponse> rows = combinedService.list(year);
        if (unitType != null) {
            java.util.Set<String> allowedCodes = auditObjectUnitService.list().stream()
                    .filter(u -> unitType.equalsIgnoreCase(u.unitType()))
                    .map(AuditObjectUnitResponse::code)
                    .collect(java.util.stream.Collectors.toSet());
            rows = rows.stream().filter(r -> allowedCodes.contains(r.branchCode())).toList();
        }
        return rows.stream()
                .sorted(Comparator.comparing(RiskBranchScoreCombinedRowResponse::totalScore).reversed())
                .limit(effectiveLimit)
                .toList();
    }

    /** get_evidence - chi nhan findingId (khong nhan entityName tuy y) de gioi han pham vi truy cap Attachment. */
    @Transactional(readOnly = true)
    public List<EvidenceResponse> getEvidence(UUID findingId) {
        auditFindingService.get(findingId); // nem BusinessException NOT_FOUND neu findingId sai/khac tenant
        return attachmentService.listByEntity("AUDIT_FINDING", findingId).stream()
                .map(a -> new EvidenceResponse(a.getId(), a.getFileName(), a.getContentType(), a.getSizeBytes(),
                        "/api/attachments/" + a.getId() + "/download"))
                .toList();
    }

    private int resolveYearBound(boolean min) {
        return masterDataItemService.list(AuditMasterDataCategory.YEAR).stream()
                .mapToInt(item -> Integer.parseInt(item.code()))
                .reduce(min ? Integer::min : Integer::max)
                .orElseThrow(() -> new BusinessException("AUDIT_TOOLS_NO_YEAR", "Chua co danh muc Nam nao"));
    }

    private RiskCriteriaToolResponse toToolResponse(CriteriaQuantitativeResponse c) {
        return new RiskCriteriaToolResponse("quantitative", c.code(), c.name(), c.weight(), c.group1Code(), c.group2Code(),
                null, null, c.criteriaType(), c.businessThreshold(), c.viewThreshold(), c.score20(), c.score40(),
                c.score60(), c.score80(), c.score100(), c.scoringGuide(), null, null, c.active());
    }

    private RiskCriteriaToolResponse toToolResponse(CriteriaQualitativeResponse c) {
        return new RiskCriteriaToolResponse("qualitative", c.code(), c.name(), c.weight(), c.group1Code(), c.group2Code(),
                null, null, null, null, null, null, null, null, null, null, null, c.impactLevel(), c.likelihoodLevel(), c.active());
    }

    private RiskCriteriaToolResponse toToolResponse(RiskCriteriaOtherResponse c) {
        return new RiskCriteriaToolResponse("other", c.code(), c.name(), c.weight(), null, null, c.groupHoCode(),
                c.riskTypeHoCode(), null, null, null, null, null, null, null, null, null, null, null, c.active());
    }
}
