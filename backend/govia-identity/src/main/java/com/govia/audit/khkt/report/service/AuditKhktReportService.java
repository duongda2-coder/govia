package com.govia.audit.khkt.report.service;

import com.govia.audit.khkt.bp.entity.AuditKhktBpConfirmed;
import com.govia.audit.khkt.bp.entity.AuditKhktBpConfirmedSegment;
import com.govia.audit.khkt.bp.repository.AuditKhktBpConfirmedRepository;
import com.govia.audit.khkt.bp.repository.AuditKhktBpConfirmedSegmentRepository;
import com.govia.audit.khkt.common.entity.AuditKhktSelectionDecision;
import com.govia.audit.khkt.th.entity.AuditKhktThConfirmed;
import com.govia.audit.khkt.th.entity.AuditKhktThConfirmedSegment;
import com.govia.audit.khkt.th.repository.AuditKhktThConfirmedRepository;
import com.govia.audit.khkt.th.repository.AuditKhktThConfirmedSegmentRepository;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditInspectionType;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectInspectionHistory;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectInspectionHistoryRepository;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
import java.util.UUID;
import java.util.stream.Collectors;

/** "Đề xuất kế hoạch kiểm toán năm" - bao cao Excel xuat ra tu du lieu da xac nhan cua module KHKT
 * (sheet ZTC_KHKT_BCBP nguon BP2, ZTC_KHKT_BCTH nguon TH2): STT, thong tin doi tuong, 5 nam lich su
 * thanh tra/giam sat/kiem toan (T-5..T-1) x 5 loai hinh, cac linh vuc kiem toan da chon (1 cot X cho
 * moi Mang nghiep vu dang cau hinh - danh muc nay do NSD tu quan ly nen KHONG co so luong/ten co
 * dinh), phong de xuat, ket qua ra soat.
 *
 * <p>Vai o BCBP khong co truong tuong ung tren AuditKhktBpConfirmed (vd "Y kien chuyen gia") -
 * spec BCBP/BCTH giong het nhau ve cau truc (chi khac nguon BP2/TH2), nhieu kha nang BCBP duoc
 * copy tu BCTH va sot lai vai cot khong ap dung cho BP2; nhung cot nay xuat ra rong thay vi bay loi. */
@Service
public class AuditKhktReportService {

    private static final int HISTORY_YEARS_BACK = 5;
    private static final List<AuditInspectionType> REPORT_INSPECTION_TYPES =
            List.of(AuditInspectionType.TTCP, AuditInspectionType.KTNN, AuditInspectionType.TTGSNH,
                    AuditInspectionType.KTNB, AuditInspectionType.GSBKS);

    private final AuditKhktBpConfirmedRepository bpConfirmedRepository;
    private final AuditKhktBpConfirmedSegmentRepository bpConfirmedSegmentRepository;
    private final AuditKhktThConfirmedRepository thConfirmedRepository;
    private final AuditKhktThConfirmedSegmentRepository thConfirmedSegmentRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditObjectInspectionHistoryRepository inspectionHistoryRepository;
    private final ExcelExportService excelExportService;

    public AuditKhktReportService(AuditKhktBpConfirmedRepository bpConfirmedRepository,
                                   AuditKhktBpConfirmedSegmentRepository bpConfirmedSegmentRepository,
                                   AuditKhktThConfirmedRepository thConfirmedRepository,
                                   AuditKhktThConfirmedSegmentRepository thConfirmedSegmentRepository,
                                   AuditMasterDataItemRepository masterDataItemRepository,
                                   AuditObjectInspectionHistoryRepository inspectionHistoryRepository,
                                   ExcelExportService excelExportService) {
        this.bpConfirmedRepository = bpConfirmedRepository;
        this.bpConfirmedSegmentRepository = bpConfirmedSegmentRepository;
        this.thConfirmedRepository = thConfirmedRepository;
        this.thConfirmedSegmentRepository = thConfirmedSegmentRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.inspectionHistoryRepository = inspectionHistoryRepository;
        this.excelExportService = excelExportService;
    }

    /** ZTC_KHKT_BCBP: 1 dong / 1 doi tuong kiem toan, gop tu TAT CA dong BP2 (moi phong) cua doi
     * tuong đo trong nam - dung logic gop giong AuditKhktThService#sync (dong dau tien tim thay lam
     * dai dien cho cac cot vo huong, hop danh sach phong + linh vuc qua tat ca dong). */
    @Transactional(readOnly = true)
    public byte[] exportBpReport(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktBpConfirmed> rows = bpConfirmedRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        Map<UUID, List<UUID>> segmentsByRow = segmentsByRow(bpConfirmedSegmentRepository.findByTenantIdAndConfirmedIdIn(tenantId,
                rows.stream().map(AuditKhktBpConfirmed::getId).toList()), AuditKhktBpConfirmedSegment::getConfirmedId,
                AuditKhktBpConfirmedSegment::getBusinessSegmentId);
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);

        Map<String, List<AuditKhktBpConfirmed>> byObject = rows.stream()
                .collect(Collectors.groupingBy(AuditKhktBpConfirmed::getAuditObjectCode, LinkedHashMap::new, Collectors.toList()));

        List<ReportRow> reportRows = new ArrayList<>();
        for (List<AuditKhktBpConfirmed> group : byObject.values()) {
            AuditKhktBpConfirmed first = group.get(0);
            Set<String> departmentCodes = new LinkedHashSet<>();
            Set<UUID> segmentIds = new LinkedHashSet<>();
            String reviewResult = null;
            AuditKhktSelectionDecision decision = null;
            for (AuditKhktBpConfirmed row : group) {
                AuditMasterDataItem dept = departments.get(row.getDepartmentId());
                if (dept != null) {
                    departmentCodes.add(dept.getCode());
                }
                segmentIds.addAll(segmentsByRow.getOrDefault(row.getId(), List.of()));
                if (reviewResult == null && row.getReviewResult() != null) {
                    reviewResult = row.getReviewResult();
                }
                if (decision == null && row.getSelectionDecision() != null) {
                    decision = row.getSelectionDecision();
                }
            }
            List<String> segmentCodes = segmentIds.stream().map(segments::get).filter(Objects::nonNull)
                    .map(AuditMasterDataItem::getCode).toList();
            reportRows.add(new ReportRow(first.getAuditObjectCode(), first.getAuditObjectName(), first.getRiskScore(),
                    first.getRankLabel(), first.getAuditObjectUnitId(), segmentCodes, List.copyOf(departmentCodes),
                    reviewResult, null, decision == null ? null : decision.name()));
        }

        return buildReport("audit_khkt_bcbp", year, reportRows, segments.values());
    }

    /** ZTC_KHKT_BCTH: 1 dong / 1 doi tuong (TH2 da la 1 dong/1 doi tuong/1 nam san), khong can gop
     * lai; "Phong de xuat" lay lai tu BP2 (nguon goc de xuat), "Linh vuc kiem toan" la linh vuc
     * TH2 da chon (thBusinessSegmentCodes, KHONG phai linh vuc BP de xuat). */
    @Transactional(readOnly = true)
    public byte[] exportThReport(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktThConfirmed> rows = thConfirmedRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        Map<UUID, List<UUID>> segmentsByRow = segmentsByRow(thConfirmedSegmentRepository.findByTenantIdAndConfirmedIdIn(tenantId,
                rows.stream().map(AuditKhktThConfirmed::getId).toList()), AuditKhktThConfirmedSegment::getConfirmedId,
                AuditKhktThConfirmedSegment::getBusinessSegmentId);
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);

        List<AuditKhktBpConfirmed> bpRows = bpConfirmedRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        Map<String, Set<String>> proposingDepartmentsByObject = new HashMap<>();
        for (AuditKhktBpConfirmed bp : bpRows) {
            AuditMasterDataItem dept = departments.get(bp.getDepartmentId());
            if (dept != null) {
                proposingDepartmentsByObject.computeIfAbsent(bp.getAuditObjectCode(), k -> new LinkedHashSet<>()).add(dept.getCode());
            }
        }

        List<ReportRow> reportRows = new ArrayList<>();
        for (AuditKhktThConfirmed row : rows) {
            List<String> segmentCodes = segmentsByRow.getOrDefault(row.getId(), List.of()).stream()
                    .map(segments::get).filter(Objects::nonNull).map(AuditMasterDataItem::getCode).toList();
            List<String> proposingDepartments = List.copyOf(proposingDepartmentsByObject.getOrDefault(row.getAuditObjectCode(), Set.of()));
            reportRows.add(new ReportRow(row.getAuditObjectCode(), row.getAuditObjectName(), row.getRiskScore(), row.getRankLabel(),
                    row.getAuditObjectUnitId(), segmentCodes, proposingDepartments, row.getBpReviewResult(), row.getExpertOpinion(),
                    row.getProposalBasisTh()));
        }

        return buildReport("audit_khkt_bcth", year, reportRows, segments.values());
    }

    private byte[] buildReport(String sheetName, Integer year, List<ReportRow> reportRows, Collection<AuditMasterDataItem> segments) {
        Map<UUID, Map<AuditInspectionType, Set<Integer>>> historyByUnit = loadInspectionHistory(TenantContext.getTenantId(), year,
                reportRows.stream().map(ReportRow::auditObjectUnitId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<AuditMasterDataItem> sortedSegments = segments.stream()
                .sorted(Comparator.comparing(AuditMasterDataItem::getCode)).toList();

        List<ExportColumn> columns = new ArrayList<>();
        columns.add(new ExportColumn("stt", "STT"));
        columns.add(new ExportColumn("auditObjectCode", "Mã đối tượng KT"));
        columns.add(new ExportColumn("auditObjectName", "Tên đối tượng KT"));
        columns.add(new ExportColumn("riskScore", "Điểm rủi ro"));
        columns.add(new ExportColumn("rankLabel", "Xếp loại rủi ro"));
        for (AuditInspectionType type : REPORT_INSPECTION_TYPES) {
            for (int offset = HISTORY_YEARS_BACK; offset >= 1; offset--) {
                columns.add(new ExportColumn(type.name() + "_T" + offset, type.name() + " (" + (year - offset) + ")"));
            }
        }
        for (AuditMasterDataItem segment : sortedSegments) {
            columns.add(new ExportColumn("segment_" + segment.getCode(), segment.getCode()));
        }
        columns.add(new ExportColumn("proposingDepartments", "Phòng đề xuất"));
        columns.add(new ExportColumn("proposingDepartmentCount", "Số phòng đề xuất"));
        columns.add(new ExportColumn("reviewResult", "Kết quả rà soát"));
        columns.add(new ExportColumn("expertOpinion", "Ý kiến chuyên gia"));
        columns.add(new ExportColumn("selectionReason", "Lý do chọn/không chọn"));

        List<Map<String, Object>> exportRows = new ArrayList<>();
        int stt = 1;
        for (ReportRow row : reportRows) {
            Map<String, Object> map = new HashMap<>();
            map.put("stt", stt++);
            map.put("auditObjectCode", row.auditObjectCode());
            map.put("auditObjectName", row.auditObjectName());
            map.put("riskScore", row.riskScore());
            map.put("rankLabel", row.rankLabel());
            Map<AuditInspectionType, Set<Integer>> history = historyByUnit.getOrDefault(row.auditObjectUnitId(), Map.of());
            for (AuditInspectionType type : REPORT_INSPECTION_TYPES) {
                Set<Integer> years = history.getOrDefault(type, Set.of());
                for (int offset = 1; offset <= HISTORY_YEARS_BACK; offset++) {
                    map.put(type.name() + "_T" + offset, years.contains(year - offset) ? "X" : "");
                }
            }
            Set<String> rowSegments = Set.copyOf(row.segmentCodes());
            for (AuditMasterDataItem segment : sortedSegments) {
                map.put("segment_" + segment.getCode(), rowSegments.contains(segment.getCode()) ? "X" : "");
            }
            map.put("proposingDepartments", String.join(", ", row.proposingDepartments()));
            map.put("proposingDepartmentCount", row.proposingDepartments().size());
            map.put("reviewResult", row.reviewResult());
            map.put("expertOpinion", row.expertOpinion());
            map.put("selectionReason", row.selectionReason());
            exportRows.add(map);
        }

        return excelExportService.export(sheetName, columns, exportRows);
    }

    private Map<UUID, Map<AuditInspectionType, Set<Integer>>> loadInspectionHistory(UUID tenantId, Integer year, Set<UUID> unitIds) {
        if (unitIds.isEmpty()) {
            return Map.of();
        }
        int fromYear = year - HISTORY_YEARS_BACK;
        int toYear = year - 1;
        List<AuditObjectInspectionHistory> histories = inspectionHistoryRepository
                .findByTenantIdAndAuditObjectUnitIdInAndYearBetween(tenantId, unitIds, fromYear, toYear);
        Map<UUID, Map<AuditInspectionType, Set<Integer>>> result = new HashMap<>();
        for (AuditObjectInspectionHistory h : histories) {
            result.computeIfAbsent(h.getAuditObjectUnitId(), k -> new HashMap<>())
                    .computeIfAbsent(h.getInspectionType(), k -> new HashSet<>()).add(h.getYear());
        }
        return result;
    }

    private Map<UUID, AuditMasterDataItem> masterDataItemsByCategory(UUID tenantId, AuditMasterDataCategory category) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, category).stream()
                .collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private <T> Map<UUID, List<UUID>> segmentsByRow(List<T> segments, java.util.function.Function<T, UUID> rowIdFn,
                                                      java.util.function.Function<T, UUID> segmentIdFn) {
        Map<UUID, List<UUID>> result = new HashMap<>();
        for (T s : segments) {
            result.computeIfAbsent(rowIdFn.apply(s), k -> new ArrayList<>()).add(segmentIdFn.apply(s));
        }
        return result;
    }

    private record ReportRow(String auditObjectCode, String auditObjectName, BigDecimal riskScore, String rankLabel,
                              UUID auditObjectUnitId, List<String> segmentCodes, List<String> proposingDepartments,
                              String reviewResult, String expertOpinion, String selectionReason) {
    }
}
