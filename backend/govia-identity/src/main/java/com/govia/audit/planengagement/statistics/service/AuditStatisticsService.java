package com.govia.audit.planengagement.statistics.service;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.planengagement.statistics.dto.EmployeeCodeOption;
import com.govia.audit.planengagement.statistics.dto.EmployeeStatRow;
import com.govia.audit.planengagement.statistics.dto.EngagementOption;
import com.govia.audit.planengagement.statistics.dto.EngagementSegmentStatRow;
import com.govia.audit.planengagement.statistics.dto.TtssDetailRow;
import com.govia.audit.planengagement.statistics.dto.UnitDetailStatRow;
import com.govia.audit.planengagement.statistics.dto.UnitStatRow;
import com.govia.audit.planengagement.statistics.dto.YearSegmentStatRow;
import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import com.govia.audit.planengagement.ttss.repository.AuditTtssRecordRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Man hinh "Thong ke" (tcode ztc_thongke, sheet "Thống kê" cua "Tao CKT (3).xlsx") - bao cao tong
 * hop CHI DOC (khong tao/sua/xoa du lieu goc), gom du lieu san co tu AuditTtssRecord (TTSS + "Trong
 * yeu" + kien nghi da gan), AuditEngagement (Nam/Ma CKT/So QD/Xep loai rui ro/Don vi) va Employee (qua
 * AuditTtssRecord.recordUsername -> UserAccount).
 *
 * "So luong KN" o MOI bao cao duoc tinh THONG NHAT la so kien nghi (teamRecommendationId) THUC SU
 * duoc gan vao it nhat 1 dong TTSS trong pham vi dang xem (dem phan biet, bo qua null) - KHONG phai
 * tong so dong trong danh muc Kien nghi cua CKT, vi danh muc co the co kien nghi chua tung duoc dung. */
@Service
public class AuditStatisticsService {

    private final AuditTtssRecordRepository ttssRepository;
    private final AuditEngagementRepository engagementRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final ExcelExportService excelExportService;

    public AuditStatisticsService(AuditTtssRecordRepository ttssRepository, AuditEngagementRepository engagementRepository,
                                   AuditMasterDataItemRepository masterDataItemRepository, AuditObjectUnitRepository auditObjectUnitRepository,
                                   EmployeeRepository employeeRepository, UserAccountRepository userAccountRepository,
                                   ExcelExportService excelExportService) {
        this.ttssRepository = ttssRepository;
        this.engagementRepository = engagementRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.excelExportService = excelExportService;
    }

    // ===================== Lookups cho cac bo loc =====================

    @Transactional(readOnly = true)
    public List<Integer> listYearOptions() {
        UUID tenantId = TenantContext.getTenantId();
        return engagementRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(AuditEngagement::getYear).filter(Objects::nonNull).distinct().sorted(Comparator.reverseOrder()).toList();
    }

    @Transactional(readOnly = true)
    public List<EngagementOption> listEngagementOptions() {
        UUID tenantId = TenantContext.getTenantId();
        return engagementRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(e -> new EngagementOption(e.getCode())).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeCodeOption> listEmployeeOptions() {
        UUID tenantId = TenantContext.getTenantId();
        return employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId).stream()
                .map(e -> new EmployeeCodeOption(e.getEmployeeCode(), e.getFullName())).toList();
    }

    // ===================== 1: Thong ke theo nam =====================

    @Transactional(readOnly = true)
    public List<YearSegmentStatRow> statsByYear(Integer year) {
        Data data = loadData();
        List<AuditTtssRecord> scoped = year == null
                ? data.ttss
                : data.ttss.stream().filter(r -> year.equals(engagementYear(data, r))).toList();

        Map<String, List<AuditTtssRecord>> byKey = scoped.stream()
                .collect(Collectors.groupingBy(r -> engagementYear(data, r) + "|" + r.getBusinessSegmentId(), LinkedHashMap::new, Collectors.toList()));

        List<YearSegmentStatRow> rows = new ArrayList<>();
        for (List<AuditTtssRecord> group : byKey.values()) {
            AuditTtssRecord sample = group.get(0);
            AuditMasterDataItem segment = data.segments.get(sample.getBusinessSegmentId());
            rows.add(new YearSegmentStatRow(engagementYear(data, sample), segment == null ? null : segment.getCode(),
                    segment == null ? null : segment.getName(), group.size(), materialCount(group), recommendationCount(group)));
        }
        rows.sort(Comparator.comparing(YearSegmentStatRow::year, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(YearSegmentStatRow::businessSegmentCode, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    @Transactional(readOnly = true)
    public List<TtssDetailRow> ttssDetailByYearSegment(Integer year, UUID businessSegmentId) {
        Data data = loadData();
        return data.ttss.stream()
                .filter(r -> year.equals(engagementYear(data, r)) && businessSegmentId.equals(r.getBusinessSegmentId()))
                .map(r -> {
                    AuditEngagement engagement = data.engagements.get(r.getEngagementId());
                    return new TtssDetailRow(engagement == null ? null : engagement.getCode(), r.getFindingCode(), r.getFindingName(),
                            r.isMaterial(), r.getCustomerName(), r.getAmount(), r.getExceptionDate());
                })
                .toList();
    }

    // ===================== 2: Thong ke theo CKT =====================

    @Transactional(readOnly = true)
    public List<EngagementSegmentStatRow> statsByEngagement(String engagementCode) {
        Data data = loadData();
        List<AuditTtssRecord> scoped = isBlank(engagementCode)
                ? data.ttss
                : data.ttss.stream().filter(r -> engagementCode.trim().equalsIgnoreCase(engagementCode(data, r))).toList();

        Map<String, List<AuditTtssRecord>> byKey = scoped.stream()
                .collect(Collectors.groupingBy(r -> engagementCode(data, r) + "|" + r.getBusinessSegmentId(), LinkedHashMap::new, Collectors.toList()));

        List<EngagementSegmentStatRow> rows = new ArrayList<>();
        for (List<AuditTtssRecord> group : byKey.values()) {
            AuditTtssRecord sample = group.get(0);
            AuditMasterDataItem segment = data.segments.get(sample.getBusinessSegmentId());
            rows.add(new EngagementSegmentStatRow(engagementCode(data, sample), segment == null ? null : segment.getCode(),
                    segment == null ? null : segment.getName(), group.size(), materialCount(group), recommendationCount(group)));
        }
        rows.sort(Comparator.comparing(EngagementSegmentStatRow::engagementCode, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EngagementSegmentStatRow::businessSegmentCode, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    // ===================== 3: Thong ke theo thanh vien doan =====================

    @Transactional(readOnly = true)
    public List<EmployeeStatRow> statsByEmployee(String employeeCode) {
        Data data = loadData();
        List<AuditTtssRecord> scoped = data.ttss;
        if (!isBlank(employeeCode)) {
            scoped = scoped.stream().filter(r -> {
                Employee employee = employeeOf(data, r);
                return employee != null && employeeCode.trim().equalsIgnoreCase(employee.getEmployeeCode());
            }).toList();
        }

        Map<String, List<AuditTtssRecord>> byKey = scoped.stream()
                .collect(Collectors.groupingBy(r -> r.getRecordUsername() + "|" + engagementYear(data, r), LinkedHashMap::new, Collectors.toList()));

        List<EmployeeStatRow> rows = new ArrayList<>();
        for (List<AuditTtssRecord> group : byKey.values()) {
            AuditTtssRecord sample = group.get(0);
            Employee employee = employeeOf(data, sample);
            rows.add(new EmployeeStatRow(sample.getRecordUsername(), employee == null ? null : employee.getEmployeeCode(),
                    employee == null ? null : employee.getFullName(), engagementYear(data, sample), group.size(), materialCount(group),
                    recommendationCount(group)));
        }
        rows.sort(Comparator.comparing(EmployeeStatRow::employeeName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EmployeeStatRow::year, Comparator.nullsLast(Comparator.reverseOrder())));
        return rows;
    }

    // ===================== 4: Thong ke theo don vi bi kiem toan =====================

    @Transactional(readOnly = true)
    public List<UnitStatRow> statsByUnit() {
        Data data = loadData();
        Map<UUID, Long> engagementCountByUnit = data.engagements.values().stream()
                .filter(e -> e.getAuditObjectUnitId() != null)
                .collect(Collectors.groupingBy(AuditEngagement::getAuditObjectUnitId, Collectors.counting()));

        Map<UUID, List<AuditTtssRecord>> ttssByUnit = data.ttss.stream()
                .filter(r -> unitIdOf(data, r) != null)
                .collect(Collectors.groupingBy(r -> unitIdOf(data, r)));

        List<UnitStatRow> rows = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : engagementCountByUnit.entrySet()) {
            AuditObjectUnit unit = data.units.get(entry.getKey());
            List<AuditTtssRecord> group = ttssByUnit.getOrDefault(entry.getKey(), List.of());
            rows.add(new UnitStatRow(entry.getKey(), unit == null ? null : unit.getCode(), unit == null ? null : unit.getName(),
                    entry.getValue(), group.size(), materialCount(group), recommendationCount(group)));
        }
        rows.sort(Comparator.comparing(UnitStatRow::unitCode, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    @Transactional(readOnly = true)
    public List<UnitDetailStatRow> unitDetail(UUID auditObjectUnitId) {
        Data data = loadData();
        if (!data.units.containsKey(auditObjectUnitId)) {
            throw new BusinessException("AUDIT_OBJECT_UNIT_NOT_FOUND", "Khong tim thay don vi kiem toan", HttpStatus.NOT_FOUND);
        }
        List<AuditEngagement> unitEngagements = data.engagements.values().stream()
                .filter(e -> auditObjectUnitId.equals(e.getAuditObjectUnitId())).toList();
        Map<UUID, List<AuditTtssRecord>> ttssByEngagement = data.ttss.stream()
                .collect(Collectors.groupingBy(AuditTtssRecord::getEngagementId));

        List<UnitDetailStatRow> rows = new ArrayList<>();
        for (AuditEngagement engagement : unitEngagements) {
            List<AuditTtssRecord> group = ttssByEngagement.getOrDefault(engagement.getId(), List.of());
            long completed = group.stream()
                    .filter(r -> r.getRecommendationApprovalStatus() == AssignmentApprovalStatus.APPROVED)
                    .map(AuditTtssRecord::getTeamRecommendationId).filter(Objects::nonNull).distinct().count();
            rows.add(new UnitDetailStatRow(engagement.getDecisionNumber(), engagement.getYear(), group.size(), materialCount(group),
                    recommendationCount(group), completed, engagement.getRiskRank()));
        }
        rows.sort(Comparator.comparing(UnitDetailStatRow::year, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UnitDetailStatRow::decisionNumber, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    // ===================== Export Excel (moi bao cao xuat rieng theo dung du lieu dang xem) =====================

    @Transactional(readOnly = true)
    public byte[] exportByYear(Integer year) {
        List<ExportColumn> columns = List.of(
                new ExportColumn("year", "Năm"), new ExportColumn("businessSegmentCode", "Mã mảng nghiệp vụ"),
                new ExportColumn("ttssCount", "Số lượng TTSS"), new ExportColumn("materialTtssCount", "Số lượng TTSS trọng yếu"),
                new ExportColumn("recommendationCount", "Số lượng KN"));
        List<Map<String, Object>> rows = statsByYear(year).stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("year", r.year());
            row.put("businessSegmentCode", r.businessSegmentCode());
            row.put("ttssCount", r.ttssCount());
            row.put("materialTtssCount", r.materialTtssCount());
            row.put("recommendationCount", r.recommendationCount());
            return row;
        }).toList();
        return excelExportService.export("thong_ke_theo_nam", columns, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportByEngagement(String engagementCode) {
        List<ExportColumn> columns = List.of(
                new ExportColumn("engagementCode", "Mã CKT"), new ExportColumn("businessSegmentCode", "Mã mảng nghiệp vụ"),
                new ExportColumn("ttssCount", "Số lượng TTSS"), new ExportColumn("materialTtssCount", "Số lượng TTSS trọng yếu"),
                new ExportColumn("recommendationCount", "Số lượng KN"));
        List<Map<String, Object>> rows = statsByEngagement(engagementCode).stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("engagementCode", r.engagementCode());
            row.put("businessSegmentCode", r.businessSegmentCode());
            row.put("ttssCount", r.ttssCount());
            row.put("materialTtssCount", r.materialTtssCount());
            row.put("recommendationCount", r.recommendationCount());
            return row;
        }).toList();
        return excelExportService.export("thong_ke_theo_ckt", columns, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportByEmployee(String employeeCode) {
        List<ExportColumn> columns = List.of(
                new ExportColumn("employeeCode", "Mã cán bộ"), new ExportColumn("employeeName", "Tên thành viên"),
                new ExportColumn("year", "Năm"), new ExportColumn("ttssCount", "Số lượng TTSS"),
                new ExportColumn("materialTtssCount", "Số lượng TTSS trọng yếu"), new ExportColumn("recommendationCount", "Số lượng KN"));
        List<Map<String, Object>> rows = statsByEmployee(employeeCode).stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("employeeCode", r.employeeCode());
            row.put("employeeName", r.employeeName());
            row.put("year", r.year());
            row.put("ttssCount", r.ttssCount());
            row.put("materialTtssCount", r.materialTtssCount());
            row.put("recommendationCount", r.recommendationCount());
            return row;
        }).toList();
        return excelExportService.export("thong_ke_theo_thanh_vien", columns, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportByUnit() {
        List<ExportColumn> columns = List.of(
                new ExportColumn("unitCode", "Mã chi nhánh"), new ExportColumn("unitName", "Tên chi nhánh"),
                new ExportColumn("engagementCount", "Số cuộc kiểm toán"), new ExportColumn("ttssCount", "Số lượng TTSS"),
                new ExportColumn("materialTtssCount", "Số lượng TTSS trọng yếu"), new ExportColumn("recommendationCount", "Số lượng KN"));
        List<Map<String, Object>> rows = statsByUnit().stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("unitCode", r.unitCode());
            row.put("unitName", r.unitName());
            row.put("engagementCount", r.engagementCount());
            row.put("ttssCount", r.ttssCount());
            row.put("materialTtssCount", r.materialTtssCount());
            row.put("recommendationCount", r.recommendationCount());
            return row;
        }).toList();
        return excelExportService.export("thong_ke_theo_don_vi", columns, rows);
    }

    // ===================== Helpers =====================

    private long materialCount(List<AuditTtssRecord> group) {
        return group.stream().filter(AuditTtssRecord::isMaterial).count();
    }

    private long recommendationCount(List<AuditTtssRecord> group) {
        return group.stream().map(AuditTtssRecord::getTeamRecommendationId).filter(Objects::nonNull).distinct().count();
    }

    private Integer engagementYear(Data data, AuditTtssRecord record) {
        AuditEngagement engagement = data.engagements.get(record.getEngagementId());
        return engagement == null ? null : engagement.getYear();
    }

    private String engagementCode(Data data, AuditTtssRecord record) {
        AuditEngagement engagement = data.engagements.get(record.getEngagementId());
        return engagement == null ? null : engagement.getCode();
    }

    private UUID unitIdOf(Data data, AuditTtssRecord record) {
        AuditEngagement engagement = data.engagements.get(record.getEngagementId());
        return engagement == null ? null : engagement.getAuditObjectUnitId();
    }

    private Employee employeeOf(Data data, AuditTtssRecord record) {
        UUID employeeId = data.employeeIdByUsername.get(record.getRecordUsername());
        return employeeId == null ? null : data.employees.get(employeeId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Nap 1 lan toan bo du lieu can cho moi bao cao - tranh N+1 query, giong cach lam cua
     * AuditEngagementService/AuditTtssService (nap theo Map roi join trong Java). */
    private Data loadData() {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditTtssRecord> ttss = ttssRepository.findByTenantId(tenantId);
        Map<UUID, AuditEngagement> engagements = engagementRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .collect(Collectors.toMap(AuditEngagement::getId, e -> e));
        Map<UUID, AuditMasterDataItem> segments = masterDataItemRepository
                .findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT).stream()
                .collect(Collectors.toMap(AuditMasterDataItem::getId, s -> s));
        Map<UUID, AuditObjectUnit> units = auditObjectUnitRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditObjectUnit::getId, u -> u));
        Map<UUID, Employee> employees = employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        Map<String, UUID> employeeIdByUsername = userAccountRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(UserAccount::getUsername, UserAccount::getEmployeeId, (a, b) -> a));
        return new Data(ttss, engagements, segments, units, employees, employeeIdByUsername);
    }

    private record Data(List<AuditTtssRecord> ttss, Map<UUID, AuditEngagement> engagements, Map<UUID, AuditMasterDataItem> segments,
                         Map<UUID, AuditObjectUnit> units, Map<UUID, Employee> employees, Map<String, UUID> employeeIdByUsername) {
    }
}
