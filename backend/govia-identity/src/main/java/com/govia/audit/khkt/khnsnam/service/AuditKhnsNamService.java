package com.govia.audit.khkt.khnsnam.service;

import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamBatchReportRequest;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamInfoRequest;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamRowResponse;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsNamUpdateRequest;
import com.govia.audit.khkt.khnsnam.dto.AuditKhnsPbRowResponse;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNam;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNamObject;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsPosition;
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
    private final AuditKhnsPbService pbService;

    public AuditKhnsNamService(AuditKhnsNamRepository repository, AuditKhnsNamObjectRepository objectRepository,
                                EmployeeRepository employeeRepository, AuditMasterDataItemRepository masterDataItemRepository,
                                AuditKhktThConfirmedRepository thConfirmedRepository,
                                AuditKhktThConfirmedSegmentRepository thConfirmedSegmentRepository, AuditLogService auditLogService,
                                AuditKhnsPbService pbService) {
        this.repository = repository;
        this.objectRepository = objectRepository;
        this.employeeRepository = employeeRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.thConfirmedRepository = thConfirmedRepository;
        this.thConfirmedSegmentRepository = thConfirmedSegmentRepository;
        this.auditLogService = auditLogService;
        this.pbService = pbService;
    }

    @Transactional(readOnly = true)
    public List<AuditKhnsNamRowResponse> list(Integer year) {
        return list(year, false);
    }

    @Transactional(readOnly = true)
    public List<AuditKhnsNamRowResponse> list(Integer year, boolean allocatedOnly) {
        return list(year, allocatedOnly, false);
    }

    /** allocatedOnly=true (man hinh KHNS_PB): chi tra can bo da duoc phan bo di kiem toan (co it nhat 1 thang/doi tuong).
     * listedOnly=true (man hinh KHNS_NAM): chi tra can bo da duoc dua vao danh sach qua nut "Cap nhat danh sach can bo". */
    @Transactional(readOnly = true)
    public List<AuditKhnsNamRowResponse> list(Integer year, boolean allocatedOnly, boolean listedOnly) {
        UUID tenantId = TenantContext.getTenantId();
        List<Employee> employees = employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId);
        Map<UUID, AuditKhnsNam> plansByEmployee = repository.findByTenantIdAndYear(tenantId, year).stream()
                .collect(Collectors.toMap(AuditKhnsNam::getEmployeeId, p -> p));
        Map<UUID, List<String>> objectCodesByPlan = objectCodesByPlan(tenantId, plansByEmployee.values());
        Map<UUID, AuditMasterDataItem> positions = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.POSITION);
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);
        ThObjectLookup thLookup = buildThLookup(tenantId, year, segments);
        Map<String, Set<AuditKhnsNamRowResponse.PositionDetail>> pbPositionsByEmployee = new HashMap<>();
        Map<String, List<String>> pbMonthNamesByEmployee = new HashMap<>();
        if (listedOnly) {
            // chuc vu va ten doi tuong theo thang lay dung tu KHNS_PB (cung nguon voi man hinh do)
            pbService.listRows(year).forEach(r -> {
                pbPositionsByEmployee.computeIfAbsent(r.employeeId(), k -> new java.util.LinkedHashSet<>())
                        .add(new AuditKhnsNamRowResponse.PositionDetail(r.positions(), r.segmentNames()));
                List<String> names = pbMonthNamesByEmployee.computeIfAbsent(r.employeeId(), k -> new ArrayList<>(java.util.Collections.nCopies(12, (String) null)));
                r.months().forEach(m -> names.set(m - 1, r.auditObjectName()));
            });
        }

        return employees.stream().filter(employee -> {
            if (!listedOnly) {
                return true;
            }
            AuditKhnsNam plan = plansByEmployee.get(employee.getId());
            return plan != null && plan.isListed();
        }).map(employee -> {
            AuditKhnsNam plan = plansByEmployee.get(employee.getId());
            List<String> objectCodes = plan == null ? List.of() : objectCodesByPlan.getOrDefault(plan.getId(), List.of());
            List<AuditKhnsNamRowResponse.PositionDetail> pbPositions = List.copyOf(
                    pbPositionsByEmployee.getOrDefault(employee.getId().toString(), Set.of()));
            return toResponse(employee, plan, objectCodes, positions, departments, segments, thLookup, year, pbPositions,
                    pbMonthNamesByEmployee.get(employee.getId().toString()));
        }).filter(row -> !allocatedOnly || isAllocated(row)).toList();
    }

    /** Nut "Cap nhat danh sach can bo" o KHNS_NAM: lay danh sach can bo da phan bo o KHNS_PB (co it nhat 1 thang/doi tuong) dua vao
     * danh sach KHNS_NAM cua nam; can bo khong con duoc phan bo bi go khoi danh sach. Giu nguyen cac truong nhap tay. */
    @Transactional
    public int syncListFromAllocation(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhnsNam> plans = repository.findByTenantIdAndYear(tenantId, year);
        Map<UUID, List<String>> objectCodesByPlan = objectCodesByPlan(tenantId, plans);
        int listed = 0;
        for (AuditKhnsNam plan : plans) {
            boolean allocated = !objectCodesByPlan.getOrDefault(plan.getId(), List.of()).isEmpty()
                    || monthCodesOf(plan).stream().anyMatch(code -> code != null && !code.isBlank());
            if (plan.isListed() != allocated) {
                plan.setListed(allocated);
                repository.save(plan);
            }
            if (allocated) {
                listed++;
            }
        }
        auditLogService.record("AuditKhnsNam", null, AuditAction.UPDATE,
                "Cap nhat danh sach can bo KHNS_NAM nam " + year + " tu KHNS_PB: " + listed + " can bo");
        return listed;
    }

    /** Sua cac truong nhap tay cua 1 dong KHNS_NAM (cong viec khac, so/ngay quyet dinh, dot du kien, ghi chu) - khong dong vao
     * phan bo thang/chuc vu (lay tu KHNS_PB). */
    @Transactional
    public AuditKhnsNamRowResponse updateInfo(UUID employeeId, Integer year, AuditKhnsNamInfoRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Employee employee = employeeRepository.findById(employeeId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay nhan vien", HttpStatus.NOT_FOUND));
        AuditKhnsNam plan = repository.findByTenantIdAndYearAndEmployeeId(tenantId, year, employeeId)
                .orElseThrow(() -> new BusinessException("AUDIT_KHNS_NAM_NOT_LISTED", "Can bo chua co trong danh sach du kien nhan su nam", HttpStatus.NOT_FOUND));
        plan.setOtherDuties(request.otherDuties());
        plan.setDecisionNumber(request.decisionNumber());
        plan.setDecisionDate(request.decisionDate());
        plan.setExpectedBatch(request.expectedBatch());
        plan.setNote(request.note());
        plan = repository.save(plan);

        auditLogService.record("AuditKhnsNam", plan.getId(), AuditAction.UPDATE,
                "Cap nhat thong tin du kien nhan su KHKT nam " + year + ": " + employee.getFullName());

        List<String> objectCodes = objectCodesByPlan(tenantId, List.of(plan)).getOrDefault(plan.getId(), List.of());
        Map<UUID, AuditMasterDataItem> positions = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.POSITION);
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);
        ThObjectLookup thLookup = buildThLookup(tenantId, year, segments);
        return toResponse(employee, plan, objectCodes, positions, departments, segments, thLookup, year, List.of(), null);
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
        Map<String, AuditKhnsNamObject> existingObjects = new HashMap<>();
        if (plan.getId() != null) {
            objectRepository.findByTenantIdAndKhnsNamIdIn(tenantId, List.of(plan.getId()))
                    .forEach(o -> existingObjects.put(o.getAuditObjectCode(), o));
        }
        Map<String, AuditKhnsNamUpdateRequest.ObjectAssignment> requestedAssignments = new HashMap<>();
        if (request.objectAssignments() != null) {
            request.objectAssignments().forEach(a -> requestedAssignments.put(a.auditObjectCode(), a));
            requestedAssignments.values().forEach(a -> AuditKhnsPosition.validate(
                    AuditKhnsPosition.parse(String.join(",", a.positions() == null ? List.of() : a.positions())),
                    a.segmentCodes() == null ? List.of() : a.segmentCodes()));
        }

        // Chuc vu suy ra tu cac chuc vu chi tiet (KHNS_PB): Truong doan > Truong nhom > Thanh vien; khong co thi dung roleInTeam gui len
        AuditKhnsRoleInTeam newRole = request.roleInTeam();
        if (!requestedAssignments.isEmpty()) {
            Set<AuditKhnsPosition> all = requestedAssignments.values().stream()
                    .flatMap(a -> AuditKhnsPosition.parse(String.join(",", a.positions() == null ? List.of() : a.positions())).stream())
                    .collect(Collectors.toSet());
            if (all.contains(AuditKhnsPosition.TEAM_LEAD)) {
                newRole = AuditKhnsRoleInTeam.TEAM_LEAD;
            } else if (all.stream().anyMatch(p -> p.name().endsWith("_GROUP_LEAD"))) {
                newRole = AuditKhnsRoleInTeam.GROUP_LEAD;
            } else if (!all.isEmpty()) {
                newRole = AuditKhnsRoleInTeam.MEMBER;
            }
        }
        if (newRole == AuditKhnsRoleInTeam.TEAM_LEAD) {
            validateNoTeamLeadConflict(tenantId, year, plan.getId(), newReferencedCodes);
        }
        // KHNS_NAM doi chuc vu (khong gui chuc vu chi tiet) -> chuc vu chi tiet cu khong con dung, de KHNS_PB suy lai tu chuc vu moi
        boolean keepDetail = request.objectAssignments() != null || newRole == plan.getRoleInTeam();

        plan.setRoleInTeam(newRole);
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
            AuditKhnsNamUpdateRequest.ObjectAssignment requested = requestedAssignments.get(code);
            if (requested != null) {
                obj.setPositions(requested.positions() == null || requested.positions().isEmpty() ? null
                        : AuditKhnsPosition.join(AuditKhnsPosition.parse(String.join(",", requested.positions()))));
                obj.setSegmentCodes(requested.segmentCodes() == null || requested.segmentCodes().isEmpty() ? null
                        : String.join(",", requested.segmentCodes()));
            } else if (keepDetail && existingObjects.containsKey(code)) {
                obj.setPositions(existingObjects.get(code).getPositions());
                obj.setSegmentCodes(existingObjects.get(code).getSegmentCodes());
            }
            objectRepository.save(obj);
        }

        auditLogService.record("AuditKhnsNam", plan.getId(), AuditAction.UPDATE,
                "Cap nhat du kien nhan su KHKT nam " + year + ": " + employee.getFullName());

        Map<UUID, AuditMasterDataItem> positions = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.POSITION);
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);
        ThObjectLookup thLookup = buildThLookup(tenantId, year, segments);
        return toResponse(employee, plan, newObjectCodes, positions, departments, segments, thLookup, year, List.of(), null);
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
        return toResponse(employee, plan, objectCodes, positions, departments, segments, thLookup, year, List.of(), null);
    }

    /** "Báo cáo theo đợt" (file mau ZTC_BC_DOT) - "NHÂN SỰ CÁC ĐOÀN KIỂM TOÁN NỘI BỘ THÁNG x NĂM y": moi don vi kiem toan trong thang la
     * 1 khoi dong, moi can bo di kiem toan don vi do 1 dong. Nguon la man KHNS_PB (doi tuong + chuc vu + nghiep vu theo thang). Cac cot:
     * Lĩnh vực kiểm toán = ma cac nghiep vu NTD (ngoai QTDH/Tin dung) trong chuc vu cua can bo tai don vi; Lĩnh vực được phân công =
     * QTĐH (CE) / TD (LN) / NTD (con lai) theo "Lĩnh vực dự kiến được phân công" cua can bo; Chức vụ = chuc vu cao nhat trong doan;
     * Thời gian kiểm toán do NSD nhap (request), khong luu. */
    @Transactional(readOnly = true)
    public byte[] exportMonthlyReport(Integer year, Integer month, AuditKhnsNamBatchReportRequest request) {
        if (month == null || month < 1 || month > 12) {
            throw new BusinessException("INVALID_MONTH", "Thang khong hop le: " + month);
        }
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, Employee> employees = employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        Map<UUID, AuditMasterDataItem> departments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.DEPARTMENT);
        Map<UUID, AuditMasterDataItem> segments = masterDataItemsByCategory(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT);
        Map<String, String> periodByObject = new HashMap<>();
        if (request != null && request.unitPeriods() != null) {
            request.unitPeriods().stream().filter(p -> p.auditObjectCode() != null && p.period() != null && !p.period().isBlank())
                    .forEach(p -> periodByObject.put(p.auditObjectCode(), p.period().trim()));
        }

        Map<String, List<AuditKhnsPbRowResponse>> rowsByObject = pbService.listRows(year).stream()
                .filter(r -> r.months().contains(month))
                .collect(Collectors.groupingBy(AuditKhnsPbRowResponse::auditObjectCode));
        List<AuditKhnsNamBatchReportWriter.Unit> units = rowsByObject.entrySet().stream().map(entry -> {
            List<AuditKhnsPbRowResponse> pbRows = entry.getValue();
            List<AuditKhnsNamBatchReportWriter.Staff> staff = pbRows.stream()
                    .filter(r -> employees.containsKey(UUID.fromString(r.employeeId())))
                    .sorted(java.util.Comparator.comparingInt((AuditKhnsPbRowResponse r) -> roleRank(r.positions(), r.roleInTeam()))
                            .thenComparing(AuditKhnsPbRowResponse::employeeName))
                    .map(r -> {
                        Employee employee = employees.get(UUID.fromString(r.employeeId()));
                        AuditMasterDataItem department = employee.getDepartmentId() == null ? null : departments.get(employee.getDepartmentId());
                        AuditMasterDataItem segment = employee.getBusinessSegmentId() == null ? null : segments.get(employee.getBusinessSegmentId());
                        return new AuditKhnsNamBatchReportWriter.Staff(employee.getFullName(), department == null ? null : department.getCode(),
                                AuditKhnsPositionLabel.group(segment == null ? null : segment.getCode(), r.positions()),
                                AuditKhnsPositionLabel.batchRole(r.positions(), r.roleInTeam()));
                    }).toList();
            String segmentCodes = pbRows.stream().filter(r -> r.positions().stream().map(AuditKhnsPosition::valueOf).anyMatch(AuditKhnsPosition::isNonCredit))
                    .flatMap(r -> r.segmentCodes().stream()).distinct().collect(Collectors.joining(","));
            return new AuditKhnsNamBatchReportWriter.Unit(pbRows.get(0).auditObjectName(), segmentCodes, periodByObject.get(entry.getKey()), staff);
        }).sorted(java.util.Comparator.comparing(AuditKhnsNamBatchReportWriter.Unit::name, java.text.Collator.getInstance(java.util.Locale.forLanguageTag("vi")))).toList();

        return AuditKhnsNamBatchReportWriter.write(year, month, request == null ? null : request.decisionNumber(),
                request == null ? null : request.decisionDate(), units);
    }

    /** Thu tu trong khoi don vi: Truong doan, Truong nhom, roi thanh vien/khac. */
    private static int roleRank(List<String> positions, AuditKhnsRoleInTeam roleInTeam) {
        AuditKhnsRoleInTeam kind = AuditKhnsPositionLabel.batchRoleKind(positions, roleInTeam);
        return kind == AuditKhnsRoleInTeam.TEAM_LEAD ? 0 : kind == AuditKhnsRoleInTeam.GROUP_LEAD ? 1 : 2;
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
                                                Map<UUID, AuditMasterDataItem> segments, ThObjectLookup thLookup, Integer year,
                                                List<AuditKhnsNamRowResponse.PositionDetail> pbPositions, List<String> pbMonthNames) {
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
                totalTeams(plan, objectCodes), plan == null ? null : plan.getNote(),
                plan == null ? null : plan.getMonth1AuditObjectCode(), plan == null ? null : plan.getMonth2AuditObjectCode(),
                plan == null ? null : plan.getMonth3AuditObjectCode(), plan == null ? null : plan.getMonth4AuditObjectCode(),
                plan == null ? null : plan.getMonth5AuditObjectCode(), plan == null ? null : plan.getMonth6AuditObjectCode(),
                plan == null ? null : plan.getMonth7AuditObjectCode(), plan == null ? null : plan.getMonth8AuditObjectCode(),
                plan == null ? null : plan.getMonth9AuditObjectCode(), plan == null ? null : plan.getMonth10AuditObjectCode(),
                plan == null ? null : plan.getMonth11AuditObjectCode(), plan == null ? null : plan.getMonth12AuditObjectCode(),
                year, pbPositions, pbMonthNames != null ? pbMonthNames : monthNames(plan, thLookup));
    }

    /** "Tong so doan du kien tham gia trong nam" = so doi tuong kiem toan khac nhau can bo di trong nam (tu cac thang KHNS_PB + doi tuong da chon). */
    private int totalTeams(AuditKhnsNam plan, List<String> objectCodes) {
        Set<String> codes = new HashSet<>(objectCodes);
        if (plan != null) {
            monthCodesOf(plan).stream().filter(c -> c != null && !c.isBlank()).forEach(codes::add);
        }
        return codes.size();
    }

    private List<String> monthNames(AuditKhnsNam plan, ThObjectLookup thLookup) {
        List<String> names = new ArrayList<>();
        if (plan != null) {
            for (String code : monthCodesOf(plan)) {
                names.add(code == null || code.isBlank() ? null : thLookup.nameByCode().getOrDefault(code, code));
            }
        }
        while (names.size() < 12) {
            names.add(null);
        }
        return names;
    }

    private List<String> monthCodesOf(AuditKhnsNam plan) {
        return java.util.Arrays.asList(plan.getMonth1AuditObjectCode(), plan.getMonth2AuditObjectCode(), plan.getMonth3AuditObjectCode(),
                plan.getMonth4AuditObjectCode(), plan.getMonth5AuditObjectCode(), plan.getMonth6AuditObjectCode(), plan.getMonth7AuditObjectCode(),
                plan.getMonth8AuditObjectCode(), plan.getMonth9AuditObjectCode(), plan.getMonth10AuditObjectCode(), plan.getMonth11AuditObjectCode(),
                plan.getMonth12AuditObjectCode());
    }
}
