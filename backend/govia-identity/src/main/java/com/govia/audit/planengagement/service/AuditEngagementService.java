package com.govia.audit.planengagement.service;

import com.govia.audit.planengagement.dto.AuditEngagementRelatedUnitRequest;
import com.govia.audit.planengagement.dto.AuditEngagementRelatedUnitResponse;
import com.govia.audit.planengagement.dto.AuditEngagementRequest;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import com.govia.audit.planengagement.dto.AuditObjectUnitOption;
import com.govia.audit.planengagement.dto.EmployeeOption;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementRelatedUnit;
import com.govia.audit.planengagement.entity.AuditEngagementStatus;
import com.govia.audit.employeecapability.entity.AuditEmployeeCapability;
import com.govia.audit.employeecapability.repository.AuditEmployeeCapabilityRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRelatedUnitRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
import com.govia.core.export.WordExportService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** CRUD "Cuoc kiem toan" (CKT) + luoi con "Don vi lien quan" - sheet "khoi tao" cua Tao CKT.xlsx. */
@Service
public class AuditEngagementService {

    private static final DateTimeFormatter IMPORT_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AuditEngagementRepository repository;
    private final AuditEngagementRelatedUnitRepository relatedUnitRepository;
    private final AuditEngagementGroupRepository groupRepository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditEmployeeCapabilityRepository employeeCapabilityRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditEngagementService(AuditEngagementRepository repository, AuditEngagementRelatedUnitRepository relatedUnitRepository,
                                   AuditEngagementGroupRepository groupRepository, AuditObjectUnitRepository auditObjectUnitRepository,
                                   EmployeeRepository employeeRepository, UserAccountRepository userAccountRepository,
                                   AuditEmployeeCapabilityRepository employeeCapabilityRepository, AuditLogService auditLogService,
                                   ExcelExportService excelExportService, WordExportService wordExportService,
                                   ExcelImportService excelImportService) {
        this.repository = repository;
        this.relatedUnitRepository = relatedUnitRepository;
        this.groupRepository = groupRepository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.employeeCapabilityRepository = employeeCapabilityRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditObjectUnitOption> listAuditObjectUnitOptions() {
        UUID tenantId = TenantContext.getTenantId();
        return auditObjectUnitRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(u -> new AuditObjectUnitOption(u.getId(), u.getCode(), u.getName(), u.getUnitType())).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeOption> listEmployeeOptions() {
        UUID tenantId = TenantContext.getTenantId();
        List<Employee> employees = employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId);
        Map<UUID, String> usernames = userAccountRepository.findByEmployeeIdIn(employees.stream().map(Employee::getId).toList()).stream()
                .collect(Collectors.toMap(UserAccount::getEmployeeId, UserAccount::getUsername, (a, b) -> a));
        Map<UUID, AuditEmployeeCapability> capabilities = employeeCapabilityRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(AuditEmployeeCapability::getEmployeeId, c -> c));
        return employees.stream()
                .map(e -> {
                    AuditEmployeeCapability capability = capabilities.get(e.getId());
                    boolean truongDoan = capability != null && capability.isTruongDoanCapable();
                    boolean truongNhom = capability != null && capability.isTruongNhomCapable();
                    return new EmployeeOption(e.getId(), e.getEmployeeCode(), e.getFullName(), usernames.get(e.getId()), truongDoan, truongNhom);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEngagementResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditEngagement> items = repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        Map<UUID, AuditObjectUnit> units = unitsById(items.stream().map(AuditEngagement::getAuditObjectUnitId).toList());
        Map<UUID, Employee> employees = employeesById(items.stream().map(AuditEngagement::getTeamLeadEmployeeId).toList());
        return items.stream().map(item -> toResponse(item, units, employees)).toList();
    }

    @Transactional(readOnly = true)
    public AuditEngagementResponse get(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement item = getOwnedOrThrow(tenantId, id);
        return toResponse(item, unitsById(List.of(item.getAuditObjectUnitId())), employeesById(List.of(item.getTeamLeadEmployeeId())));
    }

    @Transactional
    public AuditEngagementResponse create(AuditEngagementRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectUnit unit = getOwnedUnitOrThrow(tenantId, request.auditObjectUnitId());
        getOwnedEmployeeOrThrow(tenantId, request.teamLeadEmployeeId());

        AuditEngagement item = new AuditEngagement();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item.setCode(generateCode(tenantId, unit, request.year()));
        item = repository.save(item);

        auditLogService.record("AuditEngagement", item.getId(), AuditAction.CREATE, "Tao cuoc kiem toan: " + item.getCode());
        return toResponse(item, unitsById(List.of(item.getAuditObjectUnitId())), employeesById(List.of(item.getTeamLeadEmployeeId())));
    }

    @Transactional
    public AuditEngagementResponse update(UUID id, AuditEngagementRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement item = getOwnedOrThrow(tenantId, id);
        getOwnedUnitOrThrow(tenantId, request.auditObjectUnitId());
        getOwnedEmployeeOrThrow(tenantId, request.teamLeadEmployeeId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditEngagement", item.getId(), AuditAction.UPDATE, "Cap nhat cuoc kiem toan: " + item.getCode());
        return toResponse(item, unitsById(List.of(item.getAuditObjectUnitId())), employeesById(List.of(item.getTeamLeadEmployeeId())));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement item = getOwnedOrThrow(tenantId, id);
        if (!groupRepository.findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(tenantId, id).isEmpty()) {
            throw new BusinessException("AUDIT_ENGAGEMENT_HAS_GROUPS", "Cuoc kiem toan da co nhom, khong the xoa");
        }
        relatedUnitRepository.deleteByAuditEngagementId(id);
        repository.delete(item);
        auditLogService.record("AuditEngagement", id, AuditAction.DELETE, "Xoa cuoc kiem toan: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public List<AuditEngagementRelatedUnitResponse> listRelatedUnits(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getOwnedOrThrow(tenantId, engagementId);
        List<AuditEngagementRelatedUnit> rows = relatedUnitRepository.findByTenantIdAndAuditEngagementId(tenantId, engagementId);
        Map<UUID, AuditObjectUnit> units = unitsById(rows.stream().map(AuditEngagementRelatedUnit::getAuditObjectUnitId).toList());
        return rows.stream().map(row -> toRelatedUnitResponse(row, engagement, units)).toList();
    }

    @Transactional
    public AuditEngagementRelatedUnitResponse addRelatedUnit(UUID engagementId, AuditEngagementRelatedUnitRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditEngagement engagement = getOwnedOrThrow(tenantId, engagementId);
        getOwnedUnitOrThrow(tenantId, request.auditObjectUnitId());
        if (relatedUnitRepository.existsByTenantIdAndAuditEngagementIdAndAuditObjectUnitId(tenantId, engagementId, request.auditObjectUnitId())) {
            throw new BusinessException("AUDIT_ENGAGEMENT_RELATED_UNIT_DUPLICATE", "Don vi nay da co trong danh sach");
        }

        AuditEngagementRelatedUnit row = new AuditEngagementRelatedUnit();
        row.setTenantId(tenantId);
        row.setAuditEngagementId(engagementId);
        row.setAuditObjectUnitId(request.auditObjectUnitId());
        row = relatedUnitRepository.save(row);

        auditLogService.record("AuditEngagementRelatedUnit", row.getId(), AuditAction.CREATE, "Them don vi lien quan cho CKT: " + engagement.getCode());
        return toRelatedUnitResponse(row, engagement, unitsById(List.of(row.getAuditObjectUnitId())));
    }

    @Transactional
    public void deleteRelatedUnit(UUID engagementId, UUID relatedUnitId) {
        UUID tenantId = TenantContext.getTenantId();
        getOwnedOrThrow(tenantId, engagementId);
        AuditEngagementRelatedUnit row = relatedUnitRepository.findById(relatedUnitId)
                .filter(r -> r.getTenantId().equals(tenantId) && r.getAuditEngagementId().equals(engagementId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_RELATED_UNIT_NOT_FOUND", "Khong tim thay don vi lien quan", HttpStatus.NOT_FOUND));
        relatedUnitRepository.delete(row);
        auditLogService.record("AuditEngagementRelatedUnit", relatedUnitId, AuditAction.DELETE, "Xoa don vi lien quan");
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_engagement", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Cuộc kiểm toán", exportColumns(), exportRows());
    }

    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), exportColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        UUID tenantId = TenantContext.getTenantId();
        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String unitCode = row.get("auditObjectUnitCode");
                String leadCode = row.get("teamLeadEmployeeCode");
                if (isBlank(unitCode) || isBlank(leadCode)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma DTKT hoac Ma truong doan");
                }
                AuditObjectUnit unit = auditObjectUnitRepository.findByTenantIdAndCode(tenantId, unitCode.trim())
                        .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_UNIT_NOT_FOUND", "Khong tim thay doi tuong kiem toan: " + unitCode));
                Employee lead = employeeRepository.findByTenantIdAndEmployeeCode(tenantId, leadCode.trim())
                        .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay truong doan: " + leadCode));

                AuditEngagementRequest request = new AuditEngagementRequest(
                        unit.getId(), parseInt(row.get("year")), parseInt(row.get("expectedMonth")), parseDate(row.get("decisionDate")),
                        lead.getId(), row.get("decisionNumber"), parseEnum(AuditEngagementStatus.class, row.get("status")),
                        emptyToNull(row.get("riskRank")), emptyToNull(row.get("name")), emptyToNull(row.get("objective")), emptyToNull(row.get("scope")),
                        parseDate(row.get("planningStartDate")), parseDate(row.get("planningEndDate")),
                        parseDate(row.get("fieldworkStartDate")), parseDate(row.get("fieldworkEndDate")),
                        parseDate(row.get("reportStartDate")), parseDate(row.get("reportEndDate")),
                        null, null, null, null, null, null);
                create(request);
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditEngagement", null, AuditAction.CREATE,
                "Import Excel cuoc kiem toan: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    /** "Ma CKT" = Loai doi tuong (unitType) + Ma DTKT (code) + Nam + STT 2 chu so, dem theo don vi + nam. */
    private String generateCode(UUID tenantId, AuditObjectUnit unit, Integer year) {
        long existing = repository.countByTenantIdAndAuditObjectUnitIdAndYear(tenantId, unit.getId(), year);
        String seq = String.format("%02d", existing + 1);
        String code = unit.getUnitType() + unit.getCode() + year + seq;
        if (repository.findByTenantIdAndCode(tenantId, code).isPresent()) {
            // truong hop hiem: 2 request chen nhau - lui lai 1 lan quet tiep theo thay vi tao trung ma
            code = unit.getUnitType() + unit.getCode() + year + String.format("%02d", existing + 2);
        }
        return code;
    }

    private void applyRequest(AuditEngagement item, AuditEngagementRequest request) {
        item.setAuditObjectUnitId(request.auditObjectUnitId());
        item.setYear(request.year());
        item.setExpectedMonth(request.expectedMonth());
        item.setDecisionDate(request.decisionDate());
        item.setTeamLeadEmployeeId(request.teamLeadEmployeeId());
        item.setDecisionNumber(request.decisionNumber());
        item.setStatus(request.status() != null ? request.status() : AuditEngagementStatus.DRAFT);
        item.setRiskRank(request.riskRank());
        item.setName(request.name());
        item.setObjective(request.objective());
        item.setScope(request.scope());
        item.setPlanningStartDate(request.planningStartDate());
        item.setPlanningEndDate(request.planningEndDate());
        item.setFieldworkStartDate(request.fieldworkStartDate());
        item.setFieldworkEndDate(request.fieldworkEndDate());
        item.setReportStartDate(request.reportStartDate());
        item.setReportEndDate(request.reportEndDate());
        item.setInfoCollectionStart(request.infoCollectionStart());
        item.setInfoCollectionEnd(request.infoCollectionEnd());
        item.setSampleRequestStart(request.sampleRequestStart());
        item.setSampleRequestEnd(request.sampleRequestEnd());
        item.setReportPlanStart(request.reportPlanStart());
        item.setReportPlanEnd(request.reportPlanEnd());
    }

    private AuditEngagement getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan", HttpStatus.NOT_FOUND));
    }

    private AuditObjectUnit getOwnedUnitOrThrow(UUID tenantId, UUID id) {
        return auditObjectUnitRepository.findById(id)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_UNIT_NOT_FOUND", "Khong tim thay doi tuong kiem toan", HttpStatus.NOT_FOUND));
    }

    private Employee getOwnedEmployeeOrThrow(UUID tenantId, UUID id) {
        return employeeRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay nhan vien", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditObjectUnit> unitsById(List<UUID> ids) {
        Set<UUID> unique = new HashSet<>(ids);
        return auditObjectUnitRepository.findAllById(unique).stream().collect(Collectors.toMap(AuditObjectUnit::getId, u -> u));
    }

    private Map<UUID, Employee> employeesById(List<UUID> ids) {
        Set<UUID> unique = new HashSet<>(ids);
        return employeeRepository.findAllById(unique).stream().collect(Collectors.toMap(Employee::getId, e -> e));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("code", "Mã CKT"),
                new ExportColumn("auditObjectUnitCode", "Mã ĐTKT"),
                new ExportColumn("unitType", "Loại đối tượng"),
                new ExportColumn("auditObjectUnitName", "Tên ĐTKT"),
                new ExportColumn("year", "Năm"),
                new ExportColumn("expectedMonth", "Tháng dự kiến"),
                new ExportColumn("decisionDate", "Ngày QĐKT"),
                new ExportColumn("teamLeadEmployeeCode", "Mã trưởng đoàn"),
                new ExportColumn("teamLeadEmployeeName", "Trưởng đoàn"),
                new ExportColumn("decisionNumber", "Số QĐ kiểm toán"),
                new ExportColumn("status", "Trạng thái"),
                new ExportColumn("riskRank", "Xếp loại rủi ro"),
                new ExportColumn("name", "Tên đợt kiểm toán"),
                new ExportColumn("objective", "Mục tiêu"),
                new ExportColumn("scope", "Phạm vi"),
                new ExportColumn("planningStartDate", "Lập kế hoạch - bắt đầu"),
                new ExportColumn("planningEndDate", "Lập kế hoạch - kết thúc"),
                new ExportColumn("fieldworkStartDate", "Thực địa - bắt đầu"),
                new ExportColumn("fieldworkEndDate", "Thực địa - kết thúc"),
                new ExportColumn("reportStartDate", "Báo cáo sau KT - bắt đầu"),
                new ExportColumn("reportEndDate", "Báo cáo sau KT - kết thúc"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditEngagement> items = repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        Map<UUID, AuditObjectUnit> units = unitsById(items.stream().map(AuditEngagement::getAuditObjectUnitId).toList());
        Map<UUID, Employee> employees = employeesById(items.stream().map(AuditEngagement::getTeamLeadEmployeeId).toList());
        return items.stream().map(item -> {
            AuditObjectUnit unit = units.get(item.getAuditObjectUnitId());
            Employee lead = employees.get(item.getTeamLeadEmployeeId());
            Map<String, Object> row = new HashMap<>();
            row.put("code", item.getCode());
            row.put("auditObjectUnitCode", unit == null ? null : unit.getCode());
            row.put("unitType", unit == null ? null : unit.getUnitType());
            row.put("auditObjectUnitName", unit == null ? null : unit.getName());
            row.put("year", item.getYear());
            row.put("expectedMonth", item.getExpectedMonth());
            row.put("decisionDate", item.getDecisionDate());
            row.put("teamLeadEmployeeCode", lead == null ? null : lead.getEmployeeCode());
            row.put("teamLeadEmployeeName", lead == null ? null : lead.getFullName());
            row.put("decisionNumber", item.getDecisionNumber());
            row.put("status", item.getStatus());
            row.put("riskRank", item.getRiskRank());
            row.put("name", item.getName());
            row.put("objective", item.getObjective());
            row.put("scope", item.getScope());
            row.put("planningStartDate", item.getPlanningStartDate());
            row.put("planningEndDate", item.getPlanningEndDate());
            row.put("fieldworkStartDate", item.getFieldworkStartDate());
            row.put("fieldworkEndDate", item.getFieldworkEndDate());
            row.put("reportStartDate", item.getReportStartDate());
            row.put("reportEndDate", item.getReportEndDate());
            return row;
        }).toList();
    }

    private AuditEngagementResponse toResponse(AuditEngagement item, Map<UUID, AuditObjectUnit> units, Map<UUID, Employee> employees) {
        AuditObjectUnit unit = units.get(item.getAuditObjectUnitId());
        Employee lead = employees.get(item.getTeamLeadEmployeeId());
        return new AuditEngagementResponse(item.getId(), item.getCode(), item.getAuditObjectUnitId(),
                unit == null ? null : unit.getCode(), unit == null ? null : unit.getName(), unit == null ? null : unit.getUnitType(),
                item.getYear(), item.getExpectedMonth(), item.getDecisionDate(), item.getTeamLeadEmployeeId(),
                lead == null ? null : lead.getEmployeeCode(), lead == null ? null : lead.getFullName(),
                item.getDecisionNumber(), item.getStatus(), item.getRiskRank(), item.getName(), item.getObjective(), item.getScope(),
                item.getPlanningStartDate(), item.getPlanningEndDate(), item.getFieldworkStartDate(), item.getFieldworkEndDate(),
                item.getReportStartDate(), item.getReportEndDate(), item.getInfoCollectionStart(), item.getInfoCollectionEnd(),
                item.getSampleRequestStart(), item.getSampleRequestEnd(), item.getReportPlanStart(), item.getReportPlanEnd());
    }

    private AuditEngagementRelatedUnitResponse toRelatedUnitResponse(AuditEngagementRelatedUnit row, AuditEngagement engagement, Map<UUID, AuditObjectUnit> units) {
        AuditObjectUnit unit = units.get(row.getAuditObjectUnitId());
        return new AuditEngagementRelatedUnitResponse(row.getId(), row.getAuditEngagementId(), engagement.getCode(), row.getAuditObjectUnitId(),
                unit == null ? null : unit.getCode(), unit == null ? null : unit.getName(), unit == null ? null : unit.getUnitType());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private Integer parseInt(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), IMPORT_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
