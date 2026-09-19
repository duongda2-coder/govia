package com.govia.audit.khkt.thang.service;

import com.govia.audit.khkt.common.entity.AuditKhktApprovalStatus;
import com.govia.audit.khkt.scale.service.AuditKhktScaleService;
import com.govia.audit.khkt.thang.dto.AuditKhktThangRowResponse;
import com.govia.audit.khkt.thang.dto.AuditKhktThangUpdateRequest;
import com.govia.audit.khkt.thang.entity.AuditKhktThang;
import com.govia.audit.khkt.thang.repository.AuditKhktThangRepository;
import com.govia.audit.khkt.th.entity.AuditKhktThConfirmed;
import com.govia.audit.khkt.th.entity.AuditKhktThConfirmedSegment;
import com.govia.audit.khkt.th.repository.AuditKhktThConfirmedRepository;
import com.govia.audit.khkt.th.repository.AuditKhktThConfirmedSegmentRepository;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** "Khai bao so thang kiem toan trong nam" (sheet ZTC_KHKT_THANG). Nguon dong: doi tuong trong TH2
 * (AuditKhktThConfirmed) cua nam duoc chon MA da duoc "Chon 3" HOAC co dien "KHKTGS sau dieu chinh"
 * (xem eligibleConfirmedRows) - dung theo FS "cot Chon 3 hoac cot KHKTGS sau dieu chinh co dien thi moi
 * sang phan Khai bao so thang kiem toan trong nam", khong phai TAT CA doi tuong cua TH2. Moi doi
 * tuong hop le ung 1 dong, khong can them/xoa dong o day - danh sach tu dong khop lai voi TH2 moi
 * lan tai (xem nut "Cap nhat" o FE de nguoi dung chu dong lam moi khi TH2 thay doi). Cac cot doc
 * (ten, phan loai, xep hang, du no, nguon von, linh vuc kiem toan) lay nguyen tu TH2; khu vuc dia ly
 * lay tu AuditObjectUnit; quy mo tin dung/huy dong von tinh dong qua AuditKhktScaleService. Chi
 * phan thang 1-12 + ghi chu la du lieu rieng, luu trong AuditKhktThang. */
@Service
public class AuditKhktThangService {

    private final AuditKhktThangRepository thangRepository;
    private final AuditKhktThConfirmedRepository confirmedRepository;
    private final AuditKhktThConfirmedSegmentRepository confirmedSegmentRepository;
    private final AuditObjectUnitRepository objectUnitRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditKhktScaleService scaleService;
    private final AuditLogService auditLogService;

    public AuditKhktThangService(AuditKhktThangRepository thangRepository, AuditKhktThConfirmedRepository confirmedRepository,
                                  AuditKhktThConfirmedSegmentRepository confirmedSegmentRepository,
                                  AuditObjectUnitRepository objectUnitRepository, AuditMasterDataItemRepository masterDataItemRepository,
                                  AuditKhktScaleService scaleService, AuditLogService auditLogService) {
        this.thangRepository = thangRepository;
        this.confirmedRepository = confirmedRepository;
        this.confirmedSegmentRepository = confirmedSegmentRepository;
        this.objectUnitRepository = objectUnitRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.scaleService = scaleService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AuditKhktThangRowResponse> list(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktThConfirmed> confirmedRows = eligibleConfirmedRows(tenantId, year);
        if (confirmedRows.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<UUID>> segmentIdsByConfirmedId = segmentIdsByConfirmedId(tenantId,
                confirmedRows.stream().map(AuditKhktThConfirmed::getId).toList());
        Map<UUID, AuditMasterDataItem> segments = masterDataItemRepository
                .findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT).stream()
                .collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
        Map<UUID, AuditObjectUnit> unitsById = objectUnitRepository
                .findAllById(confirmedRows.stream().map(AuditKhktThConfirmed::getAuditObjectUnitId).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(AuditObjectUnit::getId, u -> u));
        Map<String, AuditKhktThang> thangByObjectCode = thangRepository.findByTenantIdAndYear(tenantId, year).stream()
                .collect(Collectors.toMap(AuditKhktThang::getAuditObjectCode, t -> t));

        return confirmedRows.stream().map(row -> {
            List<String> segmentCodes = segmentIdsByConfirmedId.getOrDefault(row.getId(), List.of()).stream()
                    .map(segments::get).filter(Objects::nonNull).map(AuditMasterDataItem::getCode).toList();
            AuditObjectUnit unit = row.getAuditObjectUnitId() == null ? null : unitsById.get(row.getAuditObjectUnitId());
            String geographicArea = unit == null ? null : unit.getGeographicArea();
            Integer creditScale = scaleService.resolveCreditScale(row.getOnBalanceSheetLoan());
            Integer fundingScale = scaleService.resolveFundingScale(row.getFundingSource());
            AuditKhktThang thang = thangByObjectCode.get(row.getAuditObjectCode());
            return toResponse(row, segmentCodes, geographicArea, creditScale, fundingScale, thang);
        }).toList();
    }

    @Transactional
    public AuditKhktThangRowResponse update(Integer year, String auditObjectCode, AuditKhktThangUpdateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditKhktThConfirmed confirmed = eligibleConfirmedRows(tenantId, year).stream()
                .filter(row -> row.getAuditObjectCode().equals(auditObjectCode)).findFirst()
                .orElseThrow(() -> new BusinessException("AUDIT_KHKT_THANG_OBJECT_NOT_FOUND",
                        "Doi tuong khong thuoc danh sach da phe duyet + 'Chon 3'/co 'KHKTGS sau dieu chinh' cua TH2 nam " + year, HttpStatus.NOT_FOUND));

        AuditKhktThang thang = thangRepository.findByTenantIdAndYearAndAuditObjectCode(tenantId, year, auditObjectCode)
                .orElseGet(() -> {
                    AuditKhktThang created = new AuditKhktThang();
                    created.setTenantId(tenantId);
                    created.setYear(year);
                    created.setAuditObjectCode(auditObjectCode);
                    return created;
                });
        thang.setMonth1(request.month1());
        thang.setMonth2(request.month2());
        thang.setMonth3(request.month3());
        thang.setMonth4(request.month4());
        thang.setMonth5(request.month5());
        thang.setMonth6(request.month6());
        thang.setMonth7(request.month7());
        thang.setMonth8(request.month8());
        thang.setMonth9(request.month9());
        thang.setMonth10(request.month10());
        thang.setMonth11(request.month11());
        thang.setMonth12(request.month12());
        thang.setNote(request.note());
        thang = thangRepository.save(thang);

        auditLogService.record("AuditKhktThang", thang.getId(), AuditAction.UPDATE,
                "Cap nhat so thang kiem toan trong nam KHKT: " + auditObjectCode + " nam " + year);

        List<UUID> segmentIds = confirmedSegmentRepository.findByTenantIdAndConfirmedIdIn(tenantId, List.of(confirmed.getId())).stream()
                .map(AuditKhktThConfirmedSegment::getBusinessSegmentId).toList();
        Map<UUID, AuditMasterDataItem> segments = masterDataItemRepository
                .findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT).stream()
                .collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
        List<String> segmentCodes = segmentIds.stream().map(segments::get).filter(Objects::nonNull).map(AuditMasterDataItem::getCode).toList();
        AuditObjectUnit unit = confirmed.getAuditObjectUnitId() == null ? null : objectUnitRepository.findById(confirmed.getAuditObjectUnitId()).orElse(null);
        String geographicArea = unit == null ? null : unit.getGeographicArea();
        Integer creditScale = scaleService.resolveCreditScale(confirmed.getOnBalanceSheetLoan());
        Integer fundingScale = scaleService.resolveFundingScale(confirmed.getFundingSource());
        return toResponse(confirmed, segmentCodes, geographicArea, creditScale, fundingScale, thang);
    }

    /** FS: chi doi tuong da "Chon 3" HOAC co dien "KHKTGS sau dieu chinh" (khac null - xem
     * AuditKhktSelectionChoice) moi sang phan Khai bao so thang kiem toan trong nam; dong thoi phai
     * da duoc PHE DUYET o Phan 2 ("Danh sach DTKT nam cua Phong ke hoach") - yeu cau test18.9. */
    private List<AuditKhktThConfirmed> eligibleConfirmedRows(UUID tenantId, Integer year) {
        return confirmedRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year).stream()
                .filter(row -> row.getApprovalStatus() == AuditKhktApprovalStatus.APPROVED)
                .filter(row -> row.isSelection3() || row.getKhktgsAfterAdjustment() != null)
                .toList();
    }

    private Map<UUID, List<UUID>> segmentIdsByConfirmedId(UUID tenantId, List<UUID> confirmedIds) {
        if (confirmedIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UUID>> result = new HashMap<>();
        for (AuditKhktThConfirmedSegment s : confirmedSegmentRepository.findByTenantIdAndConfirmedIdIn(tenantId, confirmedIds)) {
            result.computeIfAbsent(s.getConfirmedId(), k -> new ArrayList<>()).add(s.getBusinessSegmentId());
        }
        return result;
    }

    private AuditKhktThangRowResponse toResponse(AuditKhktThConfirmed row, List<String> segmentCodes, String geographicArea,
                                                  Integer creditScale, Integer fundingScale, AuditKhktThang thang) {
        return new AuditKhktThangRowResponse(row.getYear(), row.getAuditObjectCode(), row.getAuditObjectName(),
                row.getAuditObjectCategoryCode(), row.getRankLabel(), row.getOnBalanceSheetLoan(), row.getFundingSource(),
                creditScale, fundingScale, geographicArea, segmentCodes,
                thang != null && thang.isMonth1(), thang != null && thang.isMonth2(), thang != null && thang.isMonth3(),
                thang != null && thang.isMonth4(), thang != null && thang.isMonth5(), thang != null && thang.isMonth6(),
                thang != null && thang.isMonth7(), thang != null && thang.isMonth8(), thang != null && thang.isMonth9(),
                thang != null && thang.isMonth10(), thang != null && thang.isMonth11(), thang != null && thang.isMonth12(),
                thang == null ? null : thang.getNote());
    }
}
