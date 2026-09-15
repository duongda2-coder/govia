package com.govia.audit.khkt.bp.service;

import com.govia.audit.khkt.bp.dto.AuditKhktBpCandidateRequest;
import com.govia.audit.khkt.bp.dto.AuditKhktBpCandidateUpdateRequest;
import com.govia.audit.khkt.bp.dto.AuditKhktBpRowResponse;
import com.govia.audit.khkt.bp.entity.AuditKhktBpCandidate;
import com.govia.audit.khkt.bp.entity.AuditKhktBpCandidateSegment;
import com.govia.audit.khkt.bp.entity.AuditKhktBpConfirmed;
import com.govia.audit.khkt.bp.entity.AuditKhktBpConfirmedSegment;
import com.govia.audit.khkt.bp.repository.AuditKhktBpCandidateRepository;
import com.govia.audit.khkt.bp.repository.AuditKhktBpCandidateSegmentRepository;
import com.govia.audit.khkt.bp.repository.AuditKhktBpConfirmedRepository;
import com.govia.audit.khkt.bp.repository.AuditKhktBpConfirmedSegmentRepository;
import com.govia.audit.khkt.common.entity.AuditKhktApprovalStatus;
import com.govia.audit.khkt.common.entity.AuditKhktSelectionChoice;
import com.govia.audit.khkt.common.entity.AuditKhktSelectionDecision;
import com.govia.audit.khkt.common.entity.AuditKhktSourceType;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditInspectionType;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectInspectionHistory;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectInspectionHistoryRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * "De xuat DTKT nam theo phong" (sheet ZTC_KHKT_BP, 2 phan: BP = nhap/sua, BP2 = da xac nhan).
 * Phan 2 CHI duoc ghi qua confirm() - khong co create/update/delete rieng, dam bao "sua lai phai
 * thuc hien o Phan 1 roi xac nhan lai se ghi de Phan 2" dung nhu spec.
 */
@Service
public class AuditKhktBpService {

    private static final int HISTORY_YEARS_BACK = 5;

    private final AuditKhktBpCandidateRepository candidateRepository;
    private final AuditKhktBpCandidateSegmentRepository candidateSegmentRepository;
    private final AuditKhktBpConfirmedRepository confirmedRepository;
    private final AuditKhktBpConfirmedSegmentRepository confirmedSegmentRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final AuditObjectInspectionHistoryRepository inspectionHistoryRepository;
    private final AuditLogService auditLogService;

    public AuditKhktBpService(AuditKhktBpCandidateRepository candidateRepository,
                               AuditKhktBpCandidateSegmentRepository candidateSegmentRepository,
                               AuditKhktBpConfirmedRepository confirmedRepository,
                               AuditKhktBpConfirmedSegmentRepository confirmedSegmentRepository,
                               AuditMasterDataItemRepository masterDataItemRepository,
                               AuditObjectUnitRepository auditObjectUnitRepository,
                               AuditObjectInspectionHistoryRepository inspectionHistoryRepository,
                               AuditLogService auditLogService) {
        this.candidateRepository = candidateRepository;
        this.candidateSegmentRepository = candidateSegmentRepository;
        this.confirmedRepository = confirmedRepository;
        this.confirmedSegmentRepository = confirmedSegmentRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.inspectionHistoryRepository = inspectionHistoryRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AuditKhktBpRowResponse> list(UUID departmentId, Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktBpCandidate> rows = candidateRepository.findByTenantIdAndDepartmentIdAndYearOrderByAuditObjectCodeAsc(tenantId, departmentId, year);
        RowContext ctx = buildContext(tenantId, year,
                candidateSegmentsByRow(tenantId, rows.stream().map(AuditKhktBpCandidate::getId).toList()),
                rows.stream().collect(Collectors.toMap(AuditKhktBpCandidate::getId, AuditKhktBpCandidate::getAuditObjectUnitId)));
        return rows.stream().map(row -> toResponse(row.getId(), row.getDepartmentId(), row.getYear(), row.getSourceType(),
                row.getAuditObjectCode(), row.getAuditObjectName(), row.getAuditObjectCategoryCode(), row.getRiskScore(),
                row.getRankLabel(), row.getOnBalanceSheetLoan(), row.getFundingSource(), row.getReviewResult(),
                row.getSelectionDecision(), row.getProposalBasis(), row.getApprovedSelection(), row.getExpectedSelection(),
                row.getAuditScope(), row.getAdhocAuditOrSupervision(), row.getPlanAdjustment(), row.getAdjustmentReason(),
                row.getKhktgsAfterAdjustment(), null, row.getId(), ctx)).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditKhktBpRowResponse> listConfirmed(UUID departmentId, Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktBpConfirmed> rows = confirmedRepository.findByTenantIdAndDepartmentIdAndYearOrderByAuditObjectCodeAsc(tenantId, departmentId, year);
        RowContext ctx = buildContext(tenantId, year,
                confirmedSegmentsByRow(tenantId, rows.stream().map(AuditKhktBpConfirmed::getId).toList()),
                rows.stream().collect(Collectors.toMap(AuditKhktBpConfirmed::getId, AuditKhktBpConfirmed::getAuditObjectUnitId)));
        return rows.stream().map(row -> toResponse(row.getId(), row.getDepartmentId(), row.getYear(), row.getSourceType(),
                row.getAuditObjectCode(), row.getAuditObjectName(), row.getAuditObjectCategoryCode(), row.getRiskScore(),
                row.getRankLabel(), row.getOnBalanceSheetLoan(), row.getFundingSource(), row.getReviewResult(),
                row.getSelectionDecision(), row.getProposalBasis(), row.getApprovedSelection(), row.getExpectedSelection(),
                row.getAuditScope(), row.getAdhocAuditOrSupervision(), row.getPlanAdjustment(), row.getAdjustmentReason(),
                row.getKhktgsAfterAdjustment(), row.getApprovalStatus(), row.getId(), ctx)).toList();
    }

    @Transactional
    public AuditKhktBpRowResponse create(AuditKhktBpCandidateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateObject(tenantId, request.departmentId(), request.year(), request.auditObjectCode(), null);
        validateDepartment(tenantId, request.departmentId());

        AuditKhktBpCandidate item = new AuditKhktBpCandidate();
        item.setTenantId(tenantId);
        item.setDepartmentId(request.departmentId());
        item.setYear(request.year());
        item.setSourceType(request.sourceType());
        item.setAuditObjectCode(request.auditObjectCode());
        item.setAuditObjectName(request.auditObjectName());
        item.setAuditObjectCategoryCode(request.auditObjectCategoryCode());
        item.setRiskScore(request.riskScore());
        item.setRankLabel(request.rankLabel());
        Optional<AuditObjectUnit> unit = auditObjectUnitRepository.findByTenantIdAndCode(tenantId, request.auditObjectCode());
        if (unit.isPresent()) {
            item.setAuditObjectUnitId(unit.get().getId());
            item.setOnBalanceSheetLoan(unit.get().getOnBalanceSheetLoan());
            item.setFundingSource(unit.get().getFundingSource());
        }
        item = candidateRepository.save(item);
        replaceCandidateSegments(tenantId, item.getId(), request.businessSegmentIds());

        auditLogService.record("AuditKhktBpCandidate", item.getId(), AuditAction.CREATE,
                "Them de xuat DTKT nam KHKT (BP): " + item.getAuditObjectCode());
        return findResponse(request.departmentId(), request.year(), item.getId());
    }

    @Transactional
    public AuditKhktBpRowResponse update(UUID id, AuditKhktBpCandidateUpdateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditKhktBpCandidate item = getOwnedCandidateOrThrow(tenantId, id);
        item.setReviewResult(request.reviewResult());
        item.setSelectionDecision(request.selectionDecision());
        item.setProposalBasis(request.proposalBasis());
        item.setApprovedSelection(request.approvedSelection());
        item.setExpectedSelection(request.expectedSelection());
        item.setAuditScope(request.auditScope());
        item.setAdhocAuditOrSupervision(request.adhocAuditOrSupervision());
        item.setPlanAdjustment(request.planAdjustment());
        item.setAdjustmentReason(request.adjustmentReason());
        item.setKhktgsAfterAdjustment(request.khktgsAfterAdjustment());
        item = candidateRepository.save(item);
        replaceCandidateSegments(tenantId, item.getId(), request.businessSegmentIds());

        auditLogService.record("AuditKhktBpCandidate", item.getId(), AuditAction.UPDATE,
                "Cap nhat de xuat DTKT nam KHKT (BP): " + item.getAuditObjectCode());
        return findResponse(item.getDepartmentId(), item.getYear(), item.getId());
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditKhktBpCandidate item = getOwnedCandidateOrThrow(tenantId, id);
        candidateSegmentRepository.deleteByTenantIdAndCandidateId(tenantId, id);
        candidateRepository.delete(item);
        auditLogService.record("AuditKhktBpCandidate", id, AuditAction.DELETE, "Xoa de xuat DTKT nam KHKT (BP): " + item.getAuditObjectCode());
    }

    /** Nut "Xac nhan danh sach": xoa toan bo Phan 2 cu cua (phong, nam) nay, roi sao chep nguyen
     * trang tat ca dong dang co o Phan 1 sang - dung nhu spec "xac nhan lai se ghi de Phan 2". */
    @Transactional
    public void confirm(UUID departmentId, Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        validateDepartment(tenantId, departmentId);
        List<AuditKhktBpCandidate> candidates = candidateRepository.findByTenantIdAndDepartmentIdAndYearOrderByAuditObjectCodeAsc(tenantId, departmentId, year);
        Map<UUID, List<UUID>> segmentsByRow = candidateSegmentsByRow(tenantId, candidates.stream().map(AuditKhktBpCandidate::getId).toList());

        List<AuditKhktBpConfirmed> oldConfirmed = confirmedRepository.findByTenantIdAndDepartmentIdAndYearOrderByAuditObjectCodeAsc(tenantId, departmentId, year);
        for (AuditKhktBpConfirmed old : oldConfirmed) {
            confirmedSegmentRepository.deleteByTenantIdAndConfirmedId(tenantId, old.getId());
        }
        confirmedRepository.deleteByTenantIdAndDepartmentIdAndYear(tenantId, departmentId, year);

        for (AuditKhktBpCandidate candidate : candidates) {
            AuditKhktBpConfirmed confirmed = new AuditKhktBpConfirmed();
            confirmed.setTenantId(tenantId);
            confirmed.setDepartmentId(candidate.getDepartmentId());
            confirmed.setYear(candidate.getYear());
            confirmed.setSourceType(candidate.getSourceType());
            confirmed.setAuditObjectCode(candidate.getAuditObjectCode());
            confirmed.setAuditObjectName(candidate.getAuditObjectName());
            confirmed.setAuditObjectCategoryCode(candidate.getAuditObjectCategoryCode());
            confirmed.setRiskScore(candidate.getRiskScore());
            confirmed.setRankLabel(candidate.getRankLabel());
            confirmed.setAuditObjectUnitId(candidate.getAuditObjectUnitId());
            confirmed.setOnBalanceSheetLoan(candidate.getOnBalanceSheetLoan());
            confirmed.setFundingSource(candidate.getFundingSource());
            confirmed.setReviewResult(candidate.getReviewResult());
            confirmed.setSelectionDecision(candidate.getSelectionDecision());
            confirmed.setProposalBasis(candidate.getProposalBasis());
            confirmed.setApprovedSelection(candidate.getApprovedSelection());
            confirmed.setExpectedSelection(candidate.getExpectedSelection());
            confirmed.setAuditScope(candidate.getAuditScope());
            confirmed.setAdhocAuditOrSupervision(candidate.getAdhocAuditOrSupervision());
            confirmed.setPlanAdjustment(candidate.getPlanAdjustment());
            confirmed.setAdjustmentReason(candidate.getAdjustmentReason());
            confirmed.setKhktgsAfterAdjustment(candidate.getKhktgsAfterAdjustment());
            confirmed.setApprovalStatus(AuditKhktApprovalStatus.PENDING);
            confirmed = confirmedRepository.save(confirmed);

            for (UUID segmentId : segmentsByRow.getOrDefault(candidate.getId(), List.of())) {
                AuditKhktBpConfirmedSegment segment = new AuditKhktBpConfirmedSegment();
                segment.setTenantId(tenantId);
                segment.setConfirmedId(confirmed.getId());
                segment.setBusinessSegmentId(segmentId);
                confirmedSegmentRepository.save(segment);
            }
        }

        auditLogService.record("AuditKhktBpConfirmed", null, AuditAction.CREATE,
                "Xac nhan danh sach DTKT nam KHKT (BP) phong " + departmentId + " nam " + year + ": " + candidates.size() + " doi tuong");
    }

    /** Nut "Phe duyet"/"Chua phe duyet" o Phan 2 (BJ - "Trang thai") - CHI doi trang thai, khong
     * dung de sua du lieu khac cua dong da xac nhan. */
    @Transactional
    public AuditKhktBpRowResponse setApprovalStatus(UUID id, boolean approved) {
        UUID tenantId = TenantContext.getTenantId();
        AuditKhktBpConfirmed item = confirmedRepository.findById(id)
                .filter(row -> row.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_KHKT_BP_CONFIRMED_NOT_FOUND", "Khong tim thay dong da xac nhan", HttpStatus.NOT_FOUND));
        item.setApprovalStatus(approved ? AuditKhktApprovalStatus.APPROVED : AuditKhktApprovalStatus.PENDING);
        item = confirmedRepository.save(item);

        auditLogService.record("AuditKhktBpConfirmed", item.getId(), AuditAction.UPDATE,
                (approved ? "Phe duyet" : "Bo phe duyet") + " DTKT nam KHKT (BP2): " + item.getAuditObjectCode());
        return listConfirmed(item.getDepartmentId(), item.getYear()).stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    private AuditKhktBpRowResponse findResponse(UUID departmentId, Integer year, UUID rowId) {
        return list(departmentId, year).stream().filter(r -> r.id().equals(rowId)).findFirst().orElseThrow();
    }

    private void replaceCandidateSegments(UUID tenantId, UUID candidateId, List<UUID> businessSegmentIds) {
        candidateSegmentRepository.deleteByTenantIdAndCandidateId(tenantId, candidateId);
        if (businessSegmentIds == null) {
            return;
        }
        for (UUID segmentId : businessSegmentIds) {
            AuditKhktBpCandidateSegment segment = new AuditKhktBpCandidateSegment();
            segment.setTenantId(tenantId);
            segment.setCandidateId(candidateId);
            segment.setBusinessSegmentId(segmentId);
            candidateSegmentRepository.save(segment);
        }
    }

    private void checkNoDuplicateObject(UUID tenantId, UUID departmentId, Integer year, String auditObjectCode, UUID excludingId) {
        candidateRepository.findByTenantIdAndDepartmentIdAndYearAndAuditObjectCode(tenantId, departmentId, year, auditObjectCode)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_KHKT_BP_OBJECT_DUPLICATE", "Doi tuong nay da co trong danh sach de xuat: " + auditObjectCode);
                });
    }

    private void validateDepartment(UUID tenantId, UUID departmentId) {
        masterDataItemRepository.findById(departmentId)
                .filter(item -> item.getTenantId().equals(tenantId) && item.getCategory() == AuditMasterDataCategory.DEPARTMENT)
                .orElseThrow(() -> new BusinessException("DEPARTMENT_NOT_FOUND", "Khong tim thay phong nghiep vu"));
    }

    private AuditKhktBpCandidate getOwnedCandidateOrThrow(UUID tenantId, UUID id) {
        return candidateRepository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_KHKT_BP_CANDIDATE_NOT_FOUND", "Khong tim thay de xuat DTKT nam", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, List<UUID>> candidateSegmentsByRow(UUID tenantId, List<UUID> candidateIds) {
        if (candidateIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UUID>> result = new HashMap<>();
        for (AuditKhktBpCandidateSegment s : candidateSegmentRepository.findByTenantIdAndCandidateIdIn(tenantId, candidateIds)) {
            result.computeIfAbsent(s.getCandidateId(), k -> new ArrayList<>()).add(s.getBusinessSegmentId());
        }
        return result;
    }

    private Map<UUID, List<UUID>> confirmedSegmentsByRow(UUID tenantId, List<UUID> confirmedIds) {
        if (confirmedIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UUID>> result = new HashMap<>();
        for (AuditKhktBpConfirmedSegment s : confirmedSegmentRepository.findByTenantIdAndConfirmedIdIn(tenantId, confirmedIds)) {
            result.computeIfAbsent(s.getConfirmedId(), k -> new ArrayList<>()).add(s.getBusinessSegmentId());
        }
        return result;
    }

    /** Du lieu chung mot lan doc cho ca danh sach (thay vi truy van lai theo tung dong): quan he
     * quan he Danh muc phong/mang nghiep vu, va co "Lich su KT" da tinh san theo tung dong. */
    private record RowContext(Map<UUID, List<UUID>> segmentsByRowId, Map<UUID, Map<String, Boolean>> historyByRowId,
                               Map<UUID, AuditMasterDataItem> departmentsById, Map<UUID, AuditMasterDataItem> segmentsById) {
    }

    private RowContext buildContext(UUID tenantId, Integer year, Map<UUID, List<UUID>> segmentsByRowId, Map<UUID, UUID> unitIdByRowId) {
        return new RowContext(segmentsByRowId, inspectionHistoryByRow(tenantId, year, unitIdByRowId),
                masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT),
                masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT));
    }

    /** Tinh 30 co "T-5..T-1" x 6 loai hinh cho tung dong, dua tren AuditObjectInspectionHistory
     * (chi ap dung khi dong da resolve duoc auditObjectUnitId luc them vao - xem create()). Lay 1
     * lan tat ca lich su trong khoang [year-5, year-1] cho MOI don vi lien quan, tranh N+1 query. */
    private Map<UUID, Map<String, Boolean>> inspectionHistoryByRow(UUID tenantId, Integer year, Map<UUID, UUID> unitIdByRowId) {
        Set<UUID> unitIds = unitIdByRowId.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> present;
        if (unitIds.isEmpty()) {
            present = Set.of();
        } else {
            int fromYear = year - HISTORY_YEARS_BACK;
            int toYear = year - 1;
            List<AuditObjectInspectionHistory> histories = inspectionHistoryRepository
                    .findByTenantIdAndAuditObjectUnitIdInAndYearBetween(tenantId, unitIds, fromYear, toYear);
            present = new HashSet<>();
            for (AuditObjectInspectionHistory h : histories) {
                present.add(h.getAuditObjectUnitId() + "|" + h.getYear() + "|" + h.getInspectionType());
            }
        }

        Map<UUID, Map<String, Boolean>> result = new HashMap<>();
        for (Map.Entry<UUID, UUID> entry : unitIdByRowId.entrySet()) {
            UUID rowId = entry.getKey();
            UUID unitId = entry.getValue();
            Map<String, Boolean> flags = new HashMap<>();
            for (AuditInspectionType type : AuditInspectionType.values()) {
                for (int offset = 1; offset <= HISTORY_YEARS_BACK; offset++) {
                    boolean found = unitId != null && present.contains(unitId + "|" + (year - offset) + "|" + type);
                    flags.put(type.name() + "_T" + offset, found);
                }
            }
            result.put(rowId, flags);
        }
        return result;
    }

    private Map<UUID, AuditMasterDataItem> masterDataItemsByCategory(UUID tenantId, AuditMasterDataCategory category) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, category).stream()
                .collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private AuditKhktBpRowResponse toResponse(UUID id, UUID departmentId, Integer year, AuditKhktSourceType sourceType,
                                               String auditObjectCode, String auditObjectName, String auditObjectCategoryCode,
                                               BigDecimal riskScore, String rankLabel, BigDecimal onBalanceSheetLoan,
                                               BigDecimal fundingSource, String reviewResult, AuditKhktSelectionDecision selectionDecision,
                                               String proposalBasis, AuditKhktSelectionChoice approvedSelection,
                                               AuditKhktSelectionChoice expectedSelection, String auditScope,
                                               AuditKhktSelectionChoice adhocAuditOrSupervision, AuditKhktSelectionChoice planAdjustment,
                                               String adjustmentReason, AuditKhktSelectionChoice khktgsAfterAdjustment,
                                               AuditKhktApprovalStatus approvalStatus, UUID rowId, RowContext ctx) {
        AuditMasterDataItem department = ctx.departmentsById().get(departmentId);
        List<String> segmentCodes = ctx.segmentsByRowId().getOrDefault(rowId, List.of()).stream()
                .map(ctx.segmentsById()::get).filter(Objects::nonNull).map(AuditMasterDataItem::getCode).toList();
        Map<String, Boolean> history = ctx.historyByRowId().getOrDefault(rowId, Map.of());
        return new AuditKhktBpRowResponse(id, departmentId, department == null ? null : department.getCode(),
                department == null ? null : department.getName(), year, sourceType, auditObjectCode, auditObjectName,
                auditObjectCategoryCode, riskScore, rankLabel, onBalanceSheetLoan, fundingSource, reviewResult,
                selectionDecision, proposalBasis, approvedSelection, expectedSelection, auditScope,
                adhocAuditOrSupervision, planAdjustment, adjustmentReason, khktgsAfterAdjustment, approvalStatus,
                segmentCodes, history);
    }
}
