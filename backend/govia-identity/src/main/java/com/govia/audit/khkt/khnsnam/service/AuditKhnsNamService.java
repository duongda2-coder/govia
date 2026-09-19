package com.govia.audit.khkt.khnsnam.service;

import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamRowResponse;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamUpdateRequest;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNam;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNamObject;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsRoleInTeam;
import com.govia.audit.khkt.khnsnam.repository.AuditKhnsNamObjectRepository;
import com.govia.audit.khkt.khnsnam.repository.AuditKhnsNamRepository;
import com.govia.audit.khkt.th.entity.AuditKhktThConfirmed;
import com.govia.audit.khkt.th.entity.AuditKhktThConfirmedSegment;
import com.govia.audit.khkt.th.repository.AuditKhktThConfirmedRepository;
import com.govia.audit.khkt.th.repository.AuditKhktThConfirmedSegmentRepository;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** "Dự kiến nhân sự thực hiện kiểm toán năm, đợt" (sheet ZTC_KHNS_NAM) - xem AuditKhnsNam. */
@Service
public class AuditKhnsNamService {

    private final AuditKhnsNamRepository repository;
    private final AuditKhnsNamObjectRepository objectRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditKhktThConfirmedRepository thConfirmedRepository;
    private final AuditKhktThConfirmedSegmentRepository thConfirmedSegmentRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;

    public AuditKhnsNamService(AuditKhnsNamRepository repository, AuditKhnsNamObjectRepository objectRepository,
                                EmployeeRepository employeeRepository, AuditMasterDataItemRepository masterDataItemRepository,
                                AuditKhktThConfirmedRepository thConfirmedRepository,
                                AuditKhktThConfirmedSegmentRepository thConfirmedSegmentRepository, AuditLogService auditLogService,
                                ExcelExportService excelExportService) {
        this.repository = repository;
        this.objectRepository = objectRepository;
        this.employeeRepository = employeeRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.thConfirmedRepository = thConfirmedRepository;
        this.thConfirmedSegmentRepository = thConfirmedSegmentRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
    }

    @Transactional(readOnly = true)
    public List<AuditKhnsNamRowResponse> list(Integer year) {
        return list(year, false);
    }

    /** allocatedOnly=true (man hinh KHNS_PB): chi tra can bo da duoc phan bo di kiem toan (co it nhat 1 thang/doi tuong). */
    @Transactional(readOnly = true)
    public List<AuditKhnsNamRowResponse> list(Integer year, boolean allocatedOnly) {
        UUID tenantId = TenantContext.getTenantId();
        List<Employee> employees = employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId);
        Map<UUID, AuditKhnsNam> plansByEmployee = repository.findByTenantIdAndYear(tenantId, year).stream()
                .collect(Collectors.toMap(AuditKhnsNam::getEmployeeId, p -> p));
        Map<UUID, List<String>> objectCodesByPlan = objectCodesByPlan(tenantId, plansByEmployee.values());
        Map<UUID, AuditMasterDataItem> positions = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.POSITION);
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);
        ThObjectLookup thLookup = buildThLookup(tenantId, year, segments);

        return employees.stream().map(employee -> {
            AuditKhnsNam plan = plansByEmployee.get(employee.getId());
            List<String> objectCodes = plan == null ? List.of() : objectCodesByPlan.getOrDefault(plan.getId(), List.of());
            return toResponse(employee, plan, objectCodes, positions, departments, segments, thLookup);
        }).filter(row -> !allocatedOnly || isAllocated(row)).toList();
    }

    private boolean isAllocated(AuditKhnsNamRowResponse row) {
        return !row.auditObjectCodes().isEmpty() || Stream.of(row.month1AuditObjectCode(), row.month2AuditObjectCode(),
                row.month3AuditObjectCode(), row.month4AuditObjectCode(), row.month5AuditObjectCode(), row.month6AuditObjectCode(),
                row.month7AuditObjectCode(), row.month8AuditObjectCode(), row.month9AuditObjectCode(), row.month10AuditObjectCode(),
                row.month11AuditObjectCode(), row.month12AuditObjectCode()).anyMatch(code -> code != null && !code.isBlank());
    }

    @Transactional
    public AuditKhnsNamRowResponse update(UUID employeeId, Integer year, AuditKhnsNamUpdateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Employee employee = employeeRepository.findById(employeeId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay nhan vien", HttpStatus.NOT_FOUND));

        AuditKhnsNam plan = repository.findByTenantIdAndYearAndEmployeeId(tenantId, year, employeeId).orElseGet(() -> {
            AuditKhnsNam created = new AuditKhnsNam();
            created.setTenantId(tenantId);
            created.setYear(year);
            created.setEmployeeId(employeeId);
            return created;
        });

        List<String> newObjectCodes = request.auditObjectCodes() == null ? List.of() : request.auditObjectCodes();
        Set<String> newReferencedCodes = referencedObjectCodes(newObjectCodes, request.month1AuditObjectCode(),
                request.month2AuditObjectCode(), request.month3AuditObjectCode(), request.month4AuditObjectCode(),
                request.month5AuditObjectCode(), request.month6AuditObjectCode(), request.month7AuditObjectCode(),
                request.month8AuditObjectCode(), request.month9AuditObjectCode(), request.month10AuditObjectCode(),
                request.month11AuditObjectCode(), request.month12AuditObjectCode());
        if (request.roleInTeam() == AuditKhnsRoleInTeam.TEAM_LEAD) {
            validateNoTeamLeadConflict(tenantId, year, plan.getId(), newReferencedCodes);
        }

        plan.setRoleInTeam(request.roleInTeam());
        plan.setOtherDuties(request.otherDuties());
        plan.setDecisionNumber(request.decisionNumber());
        plan.setDecisionDate(request.decisionDate());
        plan.setExpectedBatch(request.expectedBatch());
        plan.setNote(request.note());
        plan.setMonth1AuditObjectCode(request.month1AuditObjectCode());
        plan.setMonth2AuditObjectCode(request.month2AuditObjectCode());
        plan.setMonth3AuditObjectCode(request.month3AuditObjectCode());
        plan.setMonth4AuditObjectCode(request.month4AuditObjectCode());
        plan.setMonth5AuditObjectCode(request.month5AuditObjectCode());
        plan.setMonth6AuditObjectCode(request.month6AuditObjectCode());
        plan.setMonth7AuditObjectCode(request.month7AuditObjectCode());
        plan.setMonth8AuditObjectCode(request.month8AuditObjectCode());
        plan.setMonth9AuditObjectCode(request.month9AuditObjectCode());
        plan.setMonth10AuditObjectCode(request.month10AuditObjectCode());
        plan.setMonth11AuditObjectCode(request.month11AuditObjectCode());
        plan.setMonth12AuditObjectCode(request.month12AuditObjectCode());
        plan = repository.save(plan);

        objectRepository.deleteByTenantIdAndKhnsNamId(tenantId, plan.getId());
        for (String code : newObjectCodes) {
            AuditKhnsNamObject obj = new AuditKhnsNamObject();
            obj.setTenantId(tenantId);
            obj.setKhnsNamId(plan.getId());
            obj.setAuditObjectCode(code);
            objectRepository.save(obj);
        }

        auditLogService.record("AuditKhnsNam", plan.getId(), AuditAction.UPDATE,
                "Cap nhat du kien nhan su KHKT nam " + year + ": " + employee.getFullName());

        Map<UUID, AuditMasterDataItem> positions = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.POSITION);
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);
        ThObjectLookup thLookup = buildThLookup(tenantId, year, segments);
        return toResponse(employee, plan, newObjectCodes, positions, departments, segments, thLookup);
    }

    /** Dung rieng cho man hinh KHNS_PB (bao cao tong hop tu du lieu nay) - man hinh do CHI cho sua
     * cot "Ghi chu" cua rieng no, cac truong con lai chi doc theo du lieu KHNS_NAM da nhap. */
    @Transactional
    public AuditKhnsNamRowResponse updateNote(UUID employeeId, Integer year, String note) {
        UUID tenantId = TenantContext.getTenantId();
        Employee employee = employeeRepository.findById(employeeId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay nhan vien", HttpStatus.NOT_FOUND));
        AuditKhnsNam plan = repository.findByTenantIdAndYearAndEmployeeId(tenantId, year, employeeId).orElseGet(() -> {
            AuditKhnsNam created = new AuditKhnsNam();
            created.setTenantId(tenantId);
            created.setYear(year);
            created.setEmployeeId(employeeId);
            return created;
        });
        plan.setNote(note);
        plan = repository.save(plan);

        auditLogService.record("AuditKhnsNam", plan.getId(), AuditAction.UPDATE,
                "Cap nhat ghi chu phan bo nhan su KHKT (KHNS_PB) nam " + year + ": " + employee.getFullName());

        List<String> objectCodes = objectCodesByPlan(tenantId, List.of(plan)).getOrDefault(plan.getId(), List.of());
        Map<UUID, AuditMasterDataItem> positions = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.POSITION);
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);
        ThObjectLookup thLookup = buildThLookup(tenantId, year, segments);
        return toResponse(employee, plan, objectCodes, positions, departments, segments, thLookup);
    }

    /** "Báo cáo theo đợt" (sheet Báo cáo theo đợt) - "DỰ KIẾN NHÂN SỰ CÁC ĐOÀN KIỂM TOÁN NỘI BỘ
     * THÁNG X": 1 dong / 1 nhan vien, cho biet thang do di kiem toan doi tuong nao hay khong - gop
     * 2 bang "di kiem toan" / "khong di kiem toan" cua sheet goc thanh 1 bang phang duy nhat co cot
     * "Trang thai" (ExcelExportService khong ho tro 2 bang canh nhau tren cung 1 sheet). */
    @Transactional(readOnly = true)
    public byte[] exportMonthlyReport(Integer year, Integer month) {
        UUID tenantId = TenantContext.getTenantId();
        List<Employee> employees = employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId);
        Map<UUID, AuditKhnsNam> plansByEmployee = repository.findByTenantIdAndYear(tenantId, year).stream()
                .collect(Collectors.toMap(AuditKhnsNam::getEmployeeId, p -> p));
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);
        ThObjectLookup thLookup = buildThLookup(tenantId, year, segments);

        List<ExportColumn> columns = List.of(
                new ExportColumn("stt", "STT"),
                new ExportColumn("status", "Trạng thái"),
                new ExportColumn("employeeCode", "Mã cán bộ"),
                new ExportColumn("fullName", "Họ và tên"),
                new ExportColumn("departmentCode", "Phòng"),
                new ExportColumn("auditObjectName", "Đối tượng KT"),
                new ExportColumn("businessSegments", "Lĩnh vực kiểm toán"),
                new ExportColumn("roleInTeam", "Chức vụ"));

        List<Map<String, Object>> rows = new ArrayList<>();
        int stt = 1;
        for (Employee employee : employees) {
            AuditKhnsNam plan = plansByEmployee.get(employee.getId());
            String monthCode = plan == null ? null : monthAuditObjectCode(plan, month);
            AuditMasterDataItem department = employee.getDepartmentId() == null ? null : departments.get(employee.getDepartmentId());
            Map<String, Object> row = new HashMap<>();
            row.put("stt", stt++);
            row.put("status", monthCode == null ? "Không đi kiểm toán" : "Đi kiểm toán");
            row.put("employeeCode", employee.getEmployeeCode());
            row.put("fullName", employee.getFullName());
            row.put("departmentCode", department == null ? null : department.getCode());
            row.put("auditObjectName", monthCode == null ? null : thLookup.nameByCode().get(monthCode));
            row.put("businessSegments", monthCode == null ? null : String.join(", ", thLookup.segmentCodesByCode().getOrDefault(monthCode, List.of())));
            row.put("roleInTeam", plan == null || plan.getRoleInTeam() == null ? null : plan.getRoleInTeam().name());
            rows.add(row);
        }

        return excelExportService.export("audit_khns_nam_thang_" + month, columns, rows);
    }

    private String monthAuditObjectCode(AuditKhnsNam plan, int month) {
        return switch (month) {
            case 1 -> plan.getMonth1AuditObjectCode();
            case 2 -> plan.getMonth2AuditObjectCode();
            case 3 -> plan.getMonth3AuditObjectCode();
            case 4 -> plan.getMonth4AuditObjectCode();
            case 5 -> plan.getMonth5AuditObjectCode();
            case 6 -> plan.getMonth6AuditObjectCode();
            case 7 -> plan.getMonth7AuditObjectCode();
            case 8 -> plan.getMonth8AuditObjectCode();
            case 9 -> plan.getMonth9AuditObjectCode();
            case 10 -> plan.getMonth10AuditObjectCode();
            case 11 -> plan.getMonth11AuditObjectCode();
            case 12 -> plan.getMonth12AuditObjectCode();
            default -> throw new BusinessException("INVALID_MONTH", "Thang khong hop le: " + month);
        };
    }

    private Set<String> referencedObjectCodes(List<String> objectCodes, String... monthCodes) {
        Set<String> result = new HashSet<>(objectCodes);
        for (String code : monthCodes) {
            if (code != null && !code.isBlank()) {
                result.add(code);
            }
        }
        return result;
    }

    /** Dung dac ta NSD: khong cho phep 2 can bo cung duoc danh dau "Truong doan" cho CUNG 1 doi
     * tuong kiem toan trong 1 nam (K hoac cac cot thang deu tinh la "tham chieu toi doi tuong do") -
     * neu khong, ChuyenThongTinKHTH se khong biet chon ai lam teamLeadEmployeeId khi day sang
     * AuditEngagement. */
    private void validateNoTeamLeadConflict(UUID tenantId, Integer year, UUID excludingPlanId, Set<String> newReferencedCodes) {
        if (newReferencedCodes.isEmpty()) {
            return;
        }
        List<AuditKhnsNam> otherTeamLeadPlans = repository.findByTenantIdAndYear(tenantId, year).stream()
                .filter(p -> p.getRoleInTeam() == AuditKhnsRoleInTeam.TEAM_LEAD)
                .filter(p -> excludingPlanId == null || !p.getId().equals(excludingPlanId))
                .toList();
        if (otherTeamLeadPlans.isEmpty()) {
            return;
        }
        Map<UUID, List<String>> objectCodesByOtherPlan = objectCodesByPlan(tenantId, otherTeamLeadPlans);
        for (AuditKhnsNam other : otherTeamLeadPlans) {
            Set<String> otherCodes = referencedObjectCodes(objectCodesByOtherPlan.getOrDefault(other.getId(), List.of()),
                    other.getMonth1AuditObjectCode(), other.getMonth2AuditObjectCode(), other.getMonth3AuditObjectCode(),
                    other.getMonth4AuditObjectCode(), other.getMonth5AuditObjectCode(), other.getMonth6AuditObjectCode(),
                    other.getMonth7AuditObjectCode(), other.getMonth8AuditObjectCode(), other.getMonth9AuditObjectCode(),
                    other.getMonth10AuditObjectCode(), other.getMonth11AuditObjectCode(), other.getMonth12AuditObjectCode());
            Set<String> overlap = new HashSet<>(newReferencedCodes);
            overlap.retainAll(otherCodes);
            if (!overlap.isEmpty()) {
                throw new BusinessException("AUDIT_KHNS_NAM_TEAM_LEAD_CONFLICT",
                        "Da co Truong doan khac cho doi tuong kiem toan: " + String.join(", ", overlap));
            }
        }
    }

    private Map<UUID, List<String>> objectCodesByPlan(UUID tenantId, java.util.Collection<AuditKhnsNam> plans) {
        List<UUID> planIds = plans.stream().map(AuditKhnsNam::getId).toList();
        if (planIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<String>> result = new HashMap<>();
        for (AuditKhnsNamObject obj : objectRepository.findByTenantIdAndKhnsNamIdIn(tenantId, planIds)) {
            result.computeIfAbsent(obj.getKhnsNamId(), k -> new ArrayList<>()).add(obj.getAuditObjectCode());
        }
        return result;
    }

    private record ThObjectLookup(Map<String, String> nameByCode, Map<String, List<String>> segmentCodesByCode) {
    }

    private ThObjectLookup buildThLookup(UUID tenantId, Integer year, Map<UUID, AuditMasterDataItem> segments) {
        List<AuditKhktThConfirmed> thRows = thConfirmedRepository.findByTenantIdAndYearOrderByAuditObjectCodeAsc(tenantId, year);
        Map<String, String> nameByCode = thRows.stream()
                .collect(Collectors.toMap(AuditKhktThConfirmed::getAuditObjectCode, AuditKhktThConfirmed::getAuditObjectName, (a, b) -> a));
        Map<UUID, List<UUID>> segmentIdsByConfirmedId = new HashMap<>();
        for (AuditKhktThConfirmedSegment s : thConfirmedSegmentRepository.findByTenantIdAndConfirmedIdIn(tenantId,
                thRows.stream().map(AuditKhktThConfirmed::getId).toList())) {
            segmentIdsByConfirmedId.computeIfAbsent(s.getConfirmedId(), k -> new ArrayList<>()).add(s.getBusinessSegmentId());
        }
        Map<String, List<String>> segmentCodesByCode = new HashMap<>();
        for (AuditKhktThConfirmed row : thRows) {
            List<String> codes = segmentIdsByConfirmedId.getOrDefault(row.getId(), List.of()).stream()
                    .map(segments::get).filter(Objects::nonNull).map(AuditMasterDataItem::getCode).toList();
            segmentCodesByCode.put(row.getAuditObjectCode(), codes);
        }
        return new ThObjectLookup(nameByCode, segmentCodesByCode);
    }

    private Map<UUID, AuditMasterDataItem> masterDataItemsByCategory(UUID tenantId, AuditMasterDataCategory category) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, category).stream()
                .collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private AuditKhnsNamRowResponse toResponse(Employee employee, AuditKhnsNam plan, List<String> objectCodes,
                                                Map<UUID, AuditMasterDataItem> positions, Map<UUID, AuditMasterDataItem> departments,
                                                Map<UUID, AuditMasterDataItem> segments, ThObjectLookup thLookup) {
        AuditMasterDataItem position = employee.getPositionId() == null ? null : positions.get(employee.getPositionId());
        AuditMasterDataItem department = employee.getDepartmentId() == null ? null : departments.get(employee.getDepartmentId());
        AuditMasterDataItem segment = employee.getBusinessSegmentId() == null ? null : segments.get(employee.getBusinessSegmentId());
        List<String> objectNames = objectCodes.stream().map(thLookup.nameByCode()::get).filter(Objects::nonNull).toList();
        List<String> objectSegmentCodes = objectCodes.stream().flatMap(code -> thLookup.segmentCodesByCode().getOrDefault(code, List.of()).stream())
                .distinct().toList();

        return new AuditKhnsNamRowResponse(employee.getId().toString(), employee.getEmployeeCode(), employee.getFullName(),
                position == null ? null : position.getName(), department == null ? null : department.getCode(),
                employee.getAuditorClassification(), segment == null ? null : segment.getCode(), employee.isTeamLeadCapable(),
                plan == null ? null : plan.getRoleInTeam(), plan == null ? null : plan.getOtherDuties(), objectCodes, objectNames,
                objectSegmentCodes, plan == null ? null : plan.getDecisionNumber(), plan == null ? null : plan.getDecisionDate(),
                plan == null ? null : plan.getExpectedBatch(),
                objectCodes.size(), plan == null ? null : plan.getNote(),
                plan == null ? null : plan.getMonth1AuditObjectCode(), plan == null ? null : plan.getMonth2AuditObjectCode(),
                plan == null ? null : plan.getMonth3AuditObjectCode(), plan == null ? null : plan.getMonth4AuditObjectCode(),
                plan == null ? null : plan.getMonth5AuditObjectCode(), plan == null ? null : plan.getMonth6AuditObjectCode(),
                plan == null ? null : plan.getMonth7AuditObjectCode(), plan == null ? null : plan.getMonth8AuditObjectCode(),
                plan == null ? null : plan.getMonth9AuditObjectCode(), plan == null ? null : plan.getMonth10AuditObjectCode(),
                plan == null ? null : plan.getMonth11AuditObjectCode(), plan == null ? null : plan.getMonth12AuditObjectCode());
    }
}
