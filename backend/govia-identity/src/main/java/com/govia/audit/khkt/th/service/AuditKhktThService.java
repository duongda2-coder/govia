package com.govia.audit.khkt.th.service;

import com.govia.audit.khkt.bp.entity.AuditKhktBpConfirmed;
import com.govia.audit.khkt.bp.entity.AuditKhktBpConfirmedSegment;
import com.govia.audit.khkt.bp.repository.AuditKhktBpConfirmedRepository;
import com.govia.audit.khkt.bp.repository.AuditKhktBpConfirmedSegmentRepository;
import com.govia.audit.khkt.th.dto.AuditKhktThCandidateUpdateRequest;
import com.govia.audit.khkt.th.dto.AuditKhktThRowResponse;
import com.govia.audit.khkt.th.entity.AuditKhktThCandidate;
import com.govia.audit.khkt.th.entity.AuditKhktThCandidateSegment;
import com.govia.audit.khkt.th.entity.AuditKhktThConfirmed;
import com.govia.audit.khkt.th.entity.AuditKhktThConfirmedSegment;
import com.govia.audit.khkt.th.repository.AuditKhktThCandidateRepository;
import com.govia.audit.khkt.th.repository.AuditKhktThCandidateSegmentRepository;
import com.govia.audit.khkt.th.repository.AuditKhktThConfirmedRepository;
import com.govia.audit.khkt.th.repository.AuditKhktThConfirmedSegmentRepository;
import com.govia.audit.khkt.common.entity.AuditKhktApprovalStatus;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * "Danh sach DTKT nam cua Phong ke hoach" (sheet ZTC_KHKT_TH, 2 phan: TH = nhap/sua, TH2 = da xac
 * nhan - chinh la "TH2" ma THANG (ZTC_KHKT_THANG) se tham chieu toi). Khac AuditKhktBpService: TH
 * gop 1 dong / 1 doi tuong / 1 nam qua TAT CA phong, khong phai 1 dong / 1 phong.
 */
@Service
public class AuditKhktThService {

    private final AuditKhktThCandidateRepository candidateRepository;
    private final AuditKhktThCandidateSegmentRepository candidateSegmentRepository;
    private final AuditKhktThConfirmedRepository confirmedRepository;
    private final AuditKhktThConfirmedSegmentRepository confirmedSegmentRepository;
    private final AuditKhktBpConfirmedRepository bpConfirmedRepository;
    private final AuditKhktBpConfirmedSegmentRepository bpConfirmedSegmentRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditLogService auditLogService;

    public AuditKhktThService(AuditKhktThCandidateRepository candidateRepository,
                               AuditKhktThCandidateSegmentRepository candidateSegmentRepository,
                               AuditKhktThConfirmedRepository confirmedRepository,
                               AuditKhktThConfirmedSegmentRepository confirmedSegmentRepository,
                               AuditKhktBpConfirmedRepository bpConfirmedRepository,
                               AuditKhktBpConfirmedSegmentRepository bpConfirmedSegmentRepository,
                               AuditMasterDataItemRepository masterDataItemRepository, AuditLogService auditLogService) {
        this.candidateRepository = candidateRepository;
        this.candidateSegmentRepository = candidateSegmentRepository;
        this.confirmedRepository = confirmedRepository;
        this.confirmedSegmentRepository = confirmedSegmentRepository;
        this.bpConfirmedRepository = bpConfirmedRepository;
        this.bpConfirmedSegmentRepository = bpConfirmedSegmentRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AuditKhktThRowResponse> list(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktThCandidate> rows = candidateRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        Map<UUID, List<UUID>> thSegmentsByRow = thSegmentsByRow(tenantId, rows.stream().map(AuditKhktThCandidate::getId).toList());
        Map<String, BpAggregate> bpAggregates = bpAggregatesByObject(tenantId, year);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);
        return rows.stream().map(row -> toResponse(row.getId(), row.getYear(), row.getSourceType(), row.getAuditObjectCode(),
                row.getAuditObjectName(), row.getAuditObjectCategoryCode(), row.getRiskScore(), row.getRankLabel(),
                row.getOnBalanceSheetLoan(), row.getFundingSource(), row.getBpReviewResult(), row.getProposalBasisTh(),
                row.getExpertOpinion(), row.isSelection1(), row.isSelection2(), row.isSelection3(),
                row.getAuditScope(), row.getAdhocAuditOrSupervision(), row.getPlanAdjustment(), row.getAdjustmentReason(),
                row.getKhktgsAfterAdjustment(), null,
                thSegmentsByRow.getOrDefault(row.getId(), List.of()), bpAggregates.get(row.getAuditObjectCode()), segments)).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditKhktThRowResponse> listConfirmed(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktThConfirmed> rows = confirmedRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        Map<UUID, List<UUID>> thSegmentsByRow = confirmedSegmentsByRow(tenantId, rows.stream().map(AuditKhktThConfirmed::getId).toList());
        Map<String, BpAggregate> bpAggregates = bpAggregatesByObject(tenantId, year);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);
        return rows.stream().map(row -> toResponse(row.getId(), row.getYear(), row.getSourceType(), row.getAuditObjectCode(),
                row.getAuditObjectName(), row.getAuditObjectCategoryCode(), row.getRiskScore(), row.getRankLabel(),
                row.getOnBalanceSheetLoan(), row.getFundingSource(), row.getBpReviewResult(), row.getProposalBasisTh(),
                row.getExpertOpinion(), row.isSelection1(), row.isSelection2(), row.isSelection3(),
                row.getAuditScope(), row.getAdhocAuditOrSupervision(), row.getPlanAdjustment(), row.getAdjustmentReason(),
                row.getKhktgsAfterAdjustment(), row.getApprovalStatus(),
                thSegmentsByRow.getOrDefault(row.getId(), List.of()), bpAggregates.get(row.getAuditObjectCode()), segments)).toList();
    }

    /** "Tong hop tu BP2": tao moi 1 dong TH1 cho moi doi tuong CHUA co trong TH1 (voi du lieu
     * snapshot dau tien tim thay), CAP NHAT lai cac truong sao chep tu BP2 (risk score/rank/du no/
     * nguon von/bp review result) cho dong da co - KHONG dong nao bi xoa, KHONG dong nao mat du
     * lieu TH tu nhap (proposalBasisTh, expertOpinion, cac Lua chon, TH de xuat LVKT). Phong Ke
     * hoach tu xoa dong khong muon giu qua man hinh (chi xoa duoc truoc khi xac nhan). */
    @Transactional
    public void sync(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktBpConfirmed> bpRows = bpConfirmedRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        Map<String, AuditKhktBpConfirmed> firstByObject = new HashMap<>();
        for (AuditKhktBpConfirmed bp : bpRows) {
            firstByObject.putIfAbsent(bp.getAuditObjectCode(), bp);
        }

        int created = 0;
        int updated = 0;
        for (Map.Entry<String, AuditKhktBpConfirmed> entry : firstByObject.entrySet()) {
            AuditKhktBpConfirmed source = entry.getValue();
            AuditKhktThCandidate target = candidateRepository.findByTenantIdAndYearAndAuditObjectCode(tenantId, year, entry.getKey())
                    .orElse(null);
            if (target == null) {
                target = new AuditKhktThCandidate();
                target.setTenantId(tenantId);
                target.setYear(year);
                target.setAuditObjectCode(source.getAuditObjectCode());
                created++;
            } else {
                updated++;
            }
            target.setSourceType(source.getSourceType());
            target.setAuditObjectName(source.getAuditObjectName());
            target.setAuditObjectCategoryCode(source.getAuditObjectCategoryCode());
            target.setRiskScore(source.getRiskScore());
            target.setRankLabel(source.getRankLabel());
            target.setAuditObjectUnitId(source.getAuditObjectUnitId());
            target.setOnBalanceSheetLoan(source.getOnBalanceSheetLoan());
            target.setFundingSource(source.getFundingSource());
            target.setBpReviewResult(source.getReviewResult());
            candidateRepository.save(target);
        }

        auditLogService.record("AuditKhktThCandidate", null, AuditAction.CREATE,
                "Tong hop danh sach DTKT nam KHKT (TH) tu BP2 nam " + year + ": " + created + " moi, " + updated + " cap nhat");
    }

    @Transactional
    public AuditKhktThRowResponse update(UUID id, AuditKhktThCandidateUpdateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditKhktThCandidate item = getOwnedCandidateOrThrow(tenantId, id);
        item.setProposalBasisTh(request.proposalBasisTh());
        item.setExpertOpinion(request.expertOpinion());
        item.setSelection1(request.selection1());
        item.setSelection2(request.selection2());
        item.setSelection3(request.selection3());
        item.setAuditScope(scopeOf(request.thBusinessSegmentIds(), masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT)));
        item.setAdhocAuditOrSupervision(request.adhocAuditOrSupervision());
        item.setPlanAdjustment(request.planAdjustment());
        item.setAdjustmentReason(request.adjustmentReason());
        item.setKhktgsAfterAdjustment(request.khktgsAfterAdjustment());
        item = candidateRepository.save(item);
        replaceCandidateSegments(tenantId, item.getId(), request.thBusinessSegmentIds());

        auditLogService.record("AuditKhktThCandidate", item.getId(), AuditAction.UPDATE,
                "Cap nhat danh sach DTKT nam KHKT (TH): " + item.getAuditObjectCode());
        return findResponse(item.getYear(), item.getId());
    }

    private AuditKhktThRowResponse findResponse(Integer year, UUID rowId) {
        return list(year).stream().filter(r -> r.id().equals(rowId)).findFirst().orElseThrow();
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditKhktThCandidate item = getOwnedCandidateOrThrow(tenantId, id);
        candidateSegmentRepository.deleteByTenantIdAndCandidateId(tenantId, id);
        candidateRepository.delete(item);
        auditLogService.record("AuditKhktThCandidate", id, AuditAction.DELETE, "Xoa DTKT nam KHKT (TH): " + item.getAuditObjectCode());
    }

    /** Nut "Xac nhan danh sach": xoa toan bo TH2 cu cua nam nay, roi sao chep nguyen trang tat ca
     * dong dang co o TH1 sang - dung nhu spec "xac nhan lai se ghi de Phan 2". */
    @Transactional
    public void confirm(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktThCandidate> candidates = candidateRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        Map<UUID, List<UUID>> segmentsByRow = thSegmentsByRow(tenantId, candidates.stream().map(AuditKhktThCandidate::getId).toList());

        List<AuditKhktThConfirmed> oldConfirmed = confirmedRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        for (AuditKhktThConfirmed old : oldConfirmed) {
            confirmedSegmentRepository.deleteByTenantIdAndConfirmedId(tenantId, old.getId());
        }
        confirmedRepository.deleteByTenantIdAndYear(tenantId, year);

        for (AuditKhktThCandidate candidate : candidates) {
            AuditKhktThConfirmed confirmed = new AuditKhktThConfirmed();
            confirmed.setTenantId(tenantId);
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
            confirmed.setBpReviewResult(candidate.getBpReviewResult());
            confirmed.setProposalBasisTh(candidate.getProposalBasisTh());
            confirmed.setExpertOpinion(candidate.getExpertOpinion());
            confirmed.setSelection1(candidate.isSelection1());
            confirmed.setSelection2(candidate.isSelection2());
            confirmed.setSelection3(candidate.isSelection3());
            confirmed.setAuditScope(candidate.getAuditScope());
            confirmed.setAdhocAuditOrSupervision(candidate.getAdhocAuditOrSupervision());
            confirmed.setPlanAdjustment(candidate.getPlanAdjustment());
            confirmed.setAdjustmentReason(candidate.getAdjustmentReason());
            confirmed.setKhktgsAfterAdjustment(candidate.getKhktgsAfterAdjustment());
            confirmed.setApprovalStatus(AuditKhktApprovalStatus.PENDING);
            confirmed = confirmedRepository.save(confirmed);

            for (UUID segmentId : segmentsByRow.getOrDefault(candidate.getId(), List.of())) {
                AuditKhktThConfirmedSegment segment = new AuditKhktThConfirmedSegment();
                segment.setTenantId(tenantId);
                segment.setConfirmedId(confirmed.getId());
                segment.setBusinessSegmentId(segmentId);
                confirmedSegmentRepository.save(segment);
            }
        }

        auditLogService.record("AuditKhktThConfirmed", null, AuditAction.CREATE,
                "Xac nhan danh sach DTKT nam KHKT (TH) nam " + year + ": " + candidates.size() + " doi tuong");
    }

    /** Nut "Phe duyet"/"Chua phe duyet" o Phan 2 (AR - "Trang thai"). */
    @Transactional
    public AuditKhktThRowResponse setApprovalStatus(UUID id, boolean approved) {
        UUID tenantId = TenantContext.getTenantId();
        AuditKhktThConfirmed item = confirmedRepository.findById(id)
                .filter(row -> row.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_KHKT_TH_CONFIRMED_NOT_FOUND", "Khong tim thay dong da xac nhan", HttpStatus.NOT_FOUND));
        item.setApprovalStatus(approved ? AuditKhktApprovalStatus.APPROVED : AuditKhktApprovalStatus.PENDING);
        item = confirmedRepository.save(item);

        auditLogService.record("AuditKhktThConfirmed", item.getId(), AuditAction.UPDATE,
                (approved ? "Phe duyet" : "Bo phe duyet") + " DTKT nam KHKT (TH2): " + item.getAuditObjectCode());
        return listConfirmed(item.getYear()).stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    private void replaceCandidateSegments(UUID tenantId, UUID candidateId, List<UUID> businessSegmentIds) {
        candidateSegmentRepository.deleteByTenantIdAndCandidateId(tenantId, candidateId);
        if (businessSegmentIds == null) {
            return;
        }
        for (UUID segmentId : businessSegmentIds) {
            AuditKhktThCandidateSegment segment = new AuditKhktThCandidateSegment();
            segment.setTenantId(tenantId);
            segment.setCandidateId(candidateId);
            segment.setBusinessSegmentId(segmentId);
            candidateSegmentRepository.save(segment);
        }
    }

    private AuditKhktThCandidate getOwnedCandidateOrThrow(UUID tenantId, UUID id) {
        return candidateRepository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_KHKT_TH_CANDIDATE_NOT_FOUND", "Khong tim thay DTKT nam (TH)", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, List<UUID>> thSegmentsByRow(UUID tenantId, List<UUID> candidateIds) {
        if (candidateIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UUID>> result = new HashMap<>();
        for (AuditKhktThCandidateSegment s : candidateSegmentRepository.findByTenantIdAndCandidateIdIn(tenantId, candidateIds)) {
            result.computeIfAbsent(s.getCandidateId(), k -> new ArrayList<>()).add(s.getBusinessSegmentId());
        }
        return result;
    }

    private Map<UUID, List<UUID>> confirmedSegmentsByRow(UUID tenantId, List<UUID> confirmedIds) {
        if (confirmedIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UUID>> result = new HashMap<>();
        for (AuditKhktThConfirmedSegment s : confirmedSegmentRepository.findByTenantIdAndConfirmedIdIn(tenantId, confirmedIds)) {
            result.computeIfAbsent(s.getConfirmedId(), k -> new ArrayList<>()).add(s.getBusinessSegmentId());
        }
        return result;
    }

    private record BpAggregate(Set<String> departmentCodes, Set<String> segmentCodes, Map<String, Set<String>> departmentsBySegment) {
    }

    /** Gom TAT CA dong AuditKhktBpConfirmed (moi phong) theo audit_object_code cho 1 nam - tra ve
     * danh sach ma phong da de xuat + hop linh vuc ho da de xuat cho tung doi tuong ("BP de xuat"). */
    private Map<String, BpAggregate> bpAggregatesByObject(UUID tenantId, Integer year) {
        List<AuditKhktBpConfirmed> bpRows = bpConfirmedRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        if (bpRows.isEmpty()) {
            return Map.of();
        }
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);
        Map<UUID, List<UUID>> segmentsByBpRow = new HashMap<>();
        for (AuditKhktBpConfirmedSegment s : bpConfirmedSegmentRepository.findByTenantIdAndConfirmedIdIn(tenantId,
                bpRows.stream().map(AuditKhktBpConfirmed::getId).toList())) {
            segmentsByBpRow.computeIfAbsent(s.getConfirmedId(), k -> new ArrayList<>()).add(s.getBusinessSegmentId());
        }

        // Thu tu phong = thu tu danh muc (sortOrder), de ket qua "PGS,PKH,KTNB1" on dinh giua cac lan tai
        List<String> deptCodesInOrder = departments.values().stream().map(AuditMasterDataItem::getCode).toList();
        Comparator<String> deptOrder = Comparator.<String>comparingInt(code -> {
            int index = deptCodesInOrder.indexOf(code);
            return index < 0 ? Integer.MAX_VALUE : index;
        }).thenComparing(Comparator.naturalOrder());

        Map<String, BpAggregate> result = new HashMap<>();
        for (AuditKhktBpConfirmed bp : bpRows) {
            BpAggregate agg = result.computeIfAbsent(bp.getAuditObjectCode(),
                    k -> new BpAggregate(new TreeSet<>(deptOrder), new LinkedHashSet<>(), new LinkedHashMap<>()));
            AuditMasterDataItem dept = departments.get(bp.getDepartmentId());
            if (dept != null) {
                agg.departmentCodes().add(dept.getCode());
            }
            for (UUID segId : segmentsByBpRow.getOrDefault(bp.getId(), List.of())) {
                AuditMasterDataItem seg = segments.get(segId);
                if (seg != null) {
                    agg.segmentCodes().add(seg.getCode());
                    if (dept != null) {
                        agg.departmentsBySegment().computeIfAbsent(seg.getCode(), k -> new TreeSet<>(deptOrder)).add(dept.getCode());
                    }
                }
            }
        }
        return result;
    }

    private Map<UUID, AuditMasterDataItem> masterDataItemsByCategory(UUID tenantId, AuditMasterDataCategory category) {
        // LinkedHashMap: giu thu tu danh muc (sortOrder) - dung de sap xep linh vuc/phong hien thi
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, category).stream()
                .collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i, (a, b) -> a, LinkedHashMap::new));
    }

    /** Ma linh vuc TH da tich, theo thu tu danh muc, noi bang dau phay (vd "AM,CE"); null neu khong tich linh vuc nao. */
    private String scopeOf(Collection<UUID> thSegmentIds, Map<UUID, AuditMasterDataItem> segments) {
        String scope = orderedSegmentCodes(thSegmentIds, segments).stream().collect(Collectors.joining(","));
        return scope.isEmpty() ? null : scope;
    }

    private List<String> orderedSegmentCodes(Collection<UUID> thSegmentIds, Map<UUID, AuditMasterDataItem> segments) {
        if (thSegmentIds == null || thSegmentIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> wanted = new HashSet<>(thSegmentIds);
        return segments.entrySet().stream().filter(e -> wanted.contains(e.getKey())).map(e -> e.getValue().getCode()).toList();
    }

    private AuditKhktThRowResponse toResponse(UUID id, Integer year, com.govia.audit.khkt.common.entity.AuditKhktSourceType sourceType,
                                               String auditObjectCode, String auditObjectName, String auditObjectCategoryCode,
                                               java.math.BigDecimal riskScore, String rankLabel, java.math.BigDecimal onBalanceSheetLoan,
                                               java.math.BigDecimal fundingSource, String bpReviewResult, String proposalBasisTh,
                                               String expertOpinion, boolean selection1, boolean selection2, boolean selection3,
                                               String auditScope, com.govia.audit.khkt.common.entity.AuditKhktSelectionChoice adhocAuditOrSupervision,
                                               com.govia.audit.khkt.common.entity.AuditKhktSelectionChoice planAdjustment, String adjustmentReason,
                                               com.govia.audit.khkt.common.entity.AuditKhktSelectionChoice khktgsAfterAdjustment,
                                               AuditKhktApprovalStatus approvalStatus,
                                               List<UUID> thSegmentIds, BpAggregate bpAggregate, Map<UUID, AuditMasterDataItem> segments) {
        List<String> thSegmentCodes = orderedSegmentCodes(thSegmentIds, segments);
        String derivedScope = scopeOf(thSegmentIds, segments);
        Map<String, List<String>> bpDepartmentsBySegment = new LinkedHashMap<>();
        if (bpAggregate != null) {
            bpAggregate.departmentsBySegment().forEach((segmentCode, deptCodes) -> bpDepartmentsBySegment.put(segmentCode, List.copyOf(deptCodes)));
        }
        List<String> proposingDepartmentCodes = bpAggregate == null ? List.of() : List.copyOf(bpAggregate.departmentCodes());
        List<String> bpProposedSegmentCodes = bpAggregate == null ? List.of() : List.copyOf(bpAggregate.segmentCodes());
        return new AuditKhktThRowResponse(id, year, sourceType, auditObjectCode, auditObjectName, auditObjectCategoryCode,
                riskScore, rankLabel, onBalanceSheetLoan, fundingSource, bpReviewResult, proposalBasisTh, expertOpinion,
                selection1, selection2, selection3, derivedScope, adhocAuditOrSupervision, planAdjustment, adjustmentReason,
                khktgsAfterAdjustment, approvalStatus, proposingDepartmentCodes, bpProposedSegmentCodes, thSegmentCodes,
                bpDepartmentsBySegment);
    }
}
