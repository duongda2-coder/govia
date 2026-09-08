package com.govia.audit.planengagement.processengagement.service;

import com.govia.audit.employeecapability.entity.AuditEmployeeCapability;
import com.govia.audit.employeecapability.repository.AuditEmployeeCapabilityRepository;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.processengagement.dto.AuditProcessEngagementRequest;
import com.govia.audit.planengagement.processengagement.dto.AuditProcessEngagementResponse;
import com.govia.audit.planengagement.processengagement.dto.TeamLeadOption;
import com.govia.audit.planengagement.processengagement.entity.AuditProcessEngagement;
import com.govia.audit.planengagement.processengagement.repository.AuditProcessEngagementRepository;
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
import com.govia.identity.repository.EmployeeRepository;
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

/** CRUD "Cuoc kiem toan theo quy trinh/nghiep vu" - sheet "man hinh tao CKT quy trinh" cua
 * "Tao CKT (3).xlsx". */
@Service
public class AuditProcessEngagementService {

    private static final DateTimeFormatter IMPORT_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AuditProcessEngagementRepository repository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditEmployeeCapabilityRepository employeeCapabilityRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditProcessEngagementService(AuditProcessEngagementRepository repository, AuditMasterDataItemRepository masterDataItemRepository,
                                          EmployeeRepository employeeRepository, AuditEmployeeCapabilityRepository employeeCapabilityRepository,
                                          AuditLogService auditLogService, ExcelExportService excelExportService,
                                          WordExportService wordExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.employeeRepository = employeeRepository;
        this.employeeCapabilityRepository = employeeCapabilityRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<TeamLeadOption> listTeamLeadOptions() {
        UUID tenantId = TenantContext.getTenantId();
        List<Employee> employees = employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId);
        Map<UUID, AuditEmployeeCapability> capabilities = employeeCapabilityRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(AuditEmployeeCapability::getEmployeeId, c -> c));
        return employees.stream()
                .map(e -> {
                    AuditEmployeeCapability capability = capabilities.get(e.getId());
                    boolean truongDoan = capability != null && capability.isTruongDoanCapable();
                    return new TeamLeadOption(e.getId(), e.getEmployeeCode(), e.getFullName(), truongDoan);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditProcessEngagementResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditProcessEngagement> items = repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        Map<UUID, AuditMasterDataItem> segments = segmentsById(items.stream().map(AuditProcessEngagement::getBusinessSegmentId).toList());
        Map<UUID, Employee> employees = employeesById(items.stream().map(AuditProcessEngagement::getTeamLeadEmployeeId).toList());
        return items.stream().map(item -> toResponse(item, segments, employees)).toList();
    }

    @Transactional(readOnly = true)
    public AuditProcessEngagementResponse get(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditProcessEngagement item = getOwnedOrThrow(tenantId, id);
        return toResponse(item, segmentsById(List.of(item.getBusinessSegmentId())), employeesById(List.of(item.getTeamLeadEmployeeId())));
    }

    @Transactional
    public AuditProcessEngagementResponse create(AuditProcessEngagementRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditMasterDataItem segment = getOwnedSegmentOrThrow(tenantId, request.businessSegmentId());
        getOwnedEmployeeOrThrow(tenantId, request.teamLeadEmployeeId());

        AuditProcessEngagement item = new AuditProcessEngagement();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item.setCode(generateCode(tenantId, segment, request.year()));
        item = repository.save(item);

        auditLogService.record("AuditProcessEngagement", item.getId(), AuditAction.CREATE, "Tao cuoc kiem toan quy trinh: " + item.getCode());
        return toResponse(item, segmentsById(List.of(item.getBusinessSegmentId())), employeesById(List.of(item.getTeamLeadEmployeeId())));
    }

    @Transactional
    public AuditProcessEngagementResponse update(UUID id, AuditProcessEngagementRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditProcessEngagement item = getOwnedOrThrow(tenantId, id);
        getOwnedSegmentOrThrow(tenantId, request.businessSegmentId());
        getOwnedEmployeeOrThrow(tenantId, request.teamLeadEmployeeId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditProcessEngagement", item.getId(), AuditAction.UPDATE, "Cap nhat cuoc kiem toan quy trinh: " + item.getCode());
        return toResponse(item, segmentsById(List.of(item.getBusinessSegmentId())), employeesById(List.of(item.getTeamLeadEmployeeId())));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditProcessEngagement item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditProcessEngagement", id, AuditAction.DELETE, "Xoa cuoc kiem toan quy trinh: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_process_engagement", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Cuộc kiểm toán quy trình", exportColumns(), exportRows());
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
                String segmentCode = row.get("businessSegmentCode");
                String leadCode = row.get("teamLeadEmployeeCode");
                if (isBlank(segmentCode) || isBlank(leadCode)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma nghiep vu hoac Ma truong doan");
                }
                AuditMasterDataItem segment = masterDataItemRepository
                        .findByTenantIdAndCategoryAndCode(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT, segmentCode.trim())
                        .orElseThrow(() -> new BusinessException("BUSINESS_SEGMENT_NOT_FOUND", "Khong tim thay nghiep vu: " + segmentCode));
                Employee lead = employeeRepository.findByTenantIdAndEmployeeCode(tenantId, leadCode.trim())
                        .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay truong doan: " + leadCode));

                AuditProcessEngagementRequest request = new AuditProcessEngagementRequest(
                        segment.getId(), parseInt(row.get("year")), parseInt(row.get("expectedMonth")), parseDate(row.get("decisionDate")),
                        lead.getId(), row.get("decisionNumber"), emptyToNull(row.get("name")));
                create(request);
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditProcessEngagement", null, AuditAction.CREATE,
                "Import Excel cuoc kiem toan quy trinh: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    /** "Ma CKT" = "QT" (Quy trinh) + Ma nghiep vu + Nam + STT 2 chu so, dem theo nghiep vu + nam. */
    private String generateCode(UUID tenantId, AuditMasterDataItem segment, Integer year) {
        long existing = repository.countByTenantIdAndBusinessSegmentIdAndYear(tenantId, segment.getId(), year);
        String seq = String.format("%02d", existing + 1);
        String code = "QT" + segment.getCode() + year + seq;
        if (repository.findByTenantIdAndCode(tenantId, code).isPresent()) {
            // truong hop hiem: 2 request chen nhau - lui lai 1 lan quet tiep theo thay vi tao trung ma
            code = "QT" + segment.getCode() + year + String.format("%02d", existing + 2);
        }
        return code;
    }

    private void applyRequest(AuditProcessEngagement item, AuditProcessEngagementRequest request) {
        item.setBusinessSegmentId(request.businessSegmentId());
        item.setYear(request.year());
        item.setExpectedMonth(request.expectedMonth());
        item.setDecisionDate(request.decisionDate());
        item.setTeamLeadEmployeeId(request.teamLeadEmployeeId());
        item.setDecisionNumber(request.decisionNumber());
        item.setName(request.name());
    }

    private AuditProcessEngagement getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_PROCESS_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan", HttpStatus.NOT_FOUND));
    }

    private AuditMasterDataItem getOwnedSegmentOrThrow(UUID tenantId, UUID id) {
        return masterDataItemRepository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId) && item.getCategory() == AuditMasterDataCategory.BUSINESS_SEGMENT)
                .orElseThrow(() -> new BusinessException("BUSINESS_SEGMENT_NOT_FOUND", "Khong tim thay nghiep vu kiem toan", HttpStatus.NOT_FOUND));
    }

    private Employee getOwnedEmployeeOrThrow(UUID tenantId, UUID id) {
        return employeeRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay nhan vien", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> segmentsById(List<UUID> ids) {
        Set<UUID> unique = new HashSet<>(ids);
        return masterDataItemRepository.findAllById(unique).stream().collect(Collectors.toMap(AuditMasterDataItem::getId, s -> s));
    }

    private Map<UUID, Employee> employeesById(List<UUID> ids) {
        Set<UUID> unique = new HashSet<>(ids);
        return employeeRepository.findAllById(unique).stream().collect(Collectors.toMap(Employee::getId, e -> e));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("code", "Mã CKT"),
                new ExportColumn("businessSegmentCode", "Mã nghiệp vụ"),
                new ExportColumn("businessSegmentName", "Nghiệp vụ kiểm toán"),
                new ExportColumn("year", "Năm"),
                new ExportColumn("expectedMonth", "Tháng dự kiến"),
                new ExportColumn("decisionDate", "Ngày QĐKT"),
                new ExportColumn("teamLeadEmployeeCode", "Mã trưởng đoàn"),
                new ExportColumn("teamLeadEmployeeName", "Trưởng đoàn"),
                new ExportColumn("decisionNumber", "Số QĐ kiểm toán"),
                new ExportColumn("name", "Tên đợt kiểm toán"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditProcessEngagement> items = repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        Map<UUID, AuditMasterDataItem> segments = segmentsById(items.stream().map(AuditProcessEngagement::getBusinessSegmentId).toList());
        Map<UUID, Employee> employees = employeesById(items.stream().map(AuditProcessEngagement::getTeamLeadEmployeeId).toList());
        return items.stream().map(item -> {
            AuditMasterDataItem segment = segments.get(item.getBusinessSegmentId());
            Employee lead = employees.get(item.getTeamLeadEmployeeId());
            Map<String, Object> row = new HashMap<>();
            row.put("code", item.getCode());
            row.put("businessSegmentCode", segment == null ? null : segment.getCode());
            row.put("businessSegmentName", segment == null ? null : segment.getName());
            row.put("year", item.getYear());
            row.put("expectedMonth", item.getExpectedMonth());
            row.put("decisionDate", item.getDecisionDate());
            row.put("teamLeadEmployeeCode", lead == null ? null : lead.getEmployeeCode());
            row.put("teamLeadEmployeeName", lead == null ? null : lead.getFullName());
            row.put("decisionNumber", item.getDecisionNumber());
            row.put("name", item.getName());
            return row;
        }).toList();
    }

    private AuditProcessEngagementResponse toResponse(AuditProcessEngagement item, Map<UUID, AuditMasterDataItem> segments, Map<UUID, Employee> employees) {
        AuditMasterDataItem segment = segments.get(item.getBusinessSegmentId());
        Employee lead = employees.get(item.getTeamLeadEmployeeId());
        return new AuditProcessEngagementResponse(item.getId(), item.getCode(), item.getBusinessSegmentId(),
                segment == null ? null : segment.getCode(), segment == null ? null : segment.getName(),
                item.getYear(), item.getExpectedMonth(), item.getDecisionDate(), item.getTeamLeadEmployeeId(),
                lead == null ? null : lead.getEmployeeCode(), lead == null ? null : lead.getFullName(),
                item.getDecisionNumber(), item.getName());
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
}
