package com.govia.audit.employeecapability.service;

import com.govia.audit.employeecapability.dto.AuditEmployeeCapabilityItemRequest;
import com.govia.audit.employeecapability.dto.AuditEmployeeCapabilityResponse;
import com.govia.audit.employeecapability.entity.AuditEmployeeCapability;
import com.govia.audit.employeecapability.repository.AuditEmployeeCapabilityRepository;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Man hinh "Khai bao kha nang dam nhan linh vuc cua nhan vien" (sheet ZTC_KNDN). Danh sach LUON lay
 * TAT CA nhan vien tu danh muc Nhan vien sang (khong them/xoa dong thu cong) - dong "kha nang" chi
 * thuc su duoc luu (INSERT) khi NSD tick chon va bam Luu lan dau; truoc do hien thi mac dinh chua
 * tick. "Phe duyet" la 1 thao tac don, khong phai quy trinh Flowable nhieu cap: bam nut se tu dong
 * dien User/Ngay phe duyet, khong the bam lai lan 2.
 */
@Service
public class AuditEmployeeCapabilityService {

    private final AuditEmployeeCapabilityRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditEmployeeCapabilityService(AuditEmployeeCapabilityRepository repository,
                                           EmployeeRepository employeeRepository,
                                           UserAccountRepository userAccountRepository,
                                           AuditLogService auditLogService,
                                           ExcelExportService excelExportService,
                                           WordExportService wordExportService,
                                           ExcelImportService excelImportService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditEmployeeCapabilityResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        List<Employee> employees = employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId);
        Map<UUID, String> usernames = usernamesByEmployeeId(employees);
        Map<UUID, AuditEmployeeCapability> capabilities = capabilitiesByEmployeeId(tenantId);
        return employees.stream().map(e -> toResponse(e, capabilities.get(e.getId()), usernames.get(e.getId()))).toList();
    }

    /** Luu hang loat - moi dong "upsert" theo employeeId (tao moi dong "kha nang" neu nhan vien do chua co). */
    @Transactional
    public List<AuditEmployeeCapabilityResponse> bulkUpdate(List<AuditEmployeeCapabilityItemRequest> items) {
        UUID tenantId = TenantContext.getTenantId();
        for (AuditEmployeeCapabilityItemRequest item : items) {
            Employee employee = getOwnedEmployeeOrThrow(tenantId, item.employeeId());
            AuditEmployeeCapability capability = repository.findByTenantIdAndEmployeeId(tenantId, employee.getId())
                    .orElseGet(() -> {
                        AuditEmployeeCapability created = new AuditEmployeeCapability();
                        created.setTenantId(tenantId);
                        created.setEmployeeId(employee.getId());
                        return created;
                    });
            applyItem(capability, item);
            repository.save(capability);
        }
        auditLogService.record("AuditEmployeeCapability", null, AuditAction.UPDATE,
                "Cap nhat kha nang dam nhan linh vuc cho " + items.size() + " nhan vien");
        return list();
    }

    /** Phe duyet 1 dong - tick "Phe duyet" + tu dien User/Ngay phe duyet, chi thuc hien duoc 1 lan. */
    @Transactional
    public AuditEmployeeCapabilityResponse approve(UUID employeeId) {
        UUID tenantId = TenantContext.getTenantId();
        Employee employee = getOwnedEmployeeOrThrow(tenantId, employeeId);
        AuditEmployeeCapability capability = repository.findByTenantIdAndEmployeeId(tenantId, employeeId)
                .orElseGet(() -> {
                    AuditEmployeeCapability created = new AuditEmployeeCapability();
                    created.setTenantId(tenantId);
                    created.setEmployeeId(employee.getId());
                    return created;
                });
        if (capability.isApproved()) {
            throw new BusinessException("EMPLOYEE_CAPABILITY_ALREADY_APPROVED", "Nhan vien nay da duoc phe duyet");
        }
        capability.setApproved(true);
        capability.setApprovedBy(TenantContext.getCurrentUser());
        capability.setApprovedAt(Instant.now());
        capability = repository.save(capability);

        auditLogService.record("AuditEmployeeCapability", employee.getId(), AuditAction.APPROVE,
                "Phe duyet kha nang dam nhan linh vuc cua " + employee.getEmployeeCode());
        return toResponse(employee, capability, usernamesByEmployeeId(List.of(employee)).get(employee.getId()));
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_employee_capability", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Khai báo khả năng đảm nhận lĩnh vực", exportColumns(), exportRows());
    }

    /** Import khop theo "User Name" (dung dinh dang file da xuat) - chi cap nhat 14 co, khong dong nao tao/xoa nhan vien. */
    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), exportColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        UUID tenantId = TenantContext.getTenantId();
        Map<String, UUID> employeeIdsByUsername = userAccountRepository.findByTenantId(tenantId).stream()
                .filter(a -> a.getEmployeeId() != null)
                .collect(Collectors.toMap(UserAccount::getUsername, UserAccount::getEmployeeId, (a, b) -> a));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String username = row.get("username");
                if (isBlank(username)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu User Name");
                }
                UUID employeeId = employeeIdsByUsername.get(username.trim());
                if (employeeId == null) {
                    throw new BusinessException("IMPORT_EMPLOYEE_NOT_FOUND", "Khong tim thay nhan vien co User Name: " + username);
                }
                AuditEmployeeCapabilityItemRequest item = new AuditEmployeeCapabilityItemRequest(employeeId,
                        parseCheckbox(row.get("theCapable")), parseCheckbox(row.get("qtdhCapable")), parseCheckbox(row.get("hdvCapable")),
                        parseCheckbox(row.get("tcktCapable")), parseCheckbox(row.get("cnttCapable")), parseCheckbox(row.get("ttkqCapable")),
                        parseCheckbox(row.get("pcrtCapable")), parseCheckbox(row.get("ttqtCapable")), parseCheckbox(row.get("xdcbCapable")),
                        parseCheckbox(row.get("tdCapable")), parseCheckbox(row.get("truongDoanCapable")), parseCheckbox(row.get("truongNhomCapable")),
                        parseCheckbox(row.get("toGiamSatCapable")), parseCheckbox(row.get("dgclCapable")));
                bulkUpdate(List.of(item));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditEmployeeCapability", null, AuditAction.CREATE,
                "Import Excel kha nang dam nhan linh vuc: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyItem(AuditEmployeeCapability capability, AuditEmployeeCapabilityItemRequest item) {
        capability.setTheCapable(item.theCapable());
        capability.setQtdhCapable(item.qtdhCapable());
        capability.setHdvCapable(item.hdvCapable());
        capability.setTcktCapable(item.tcktCapable());
        capability.setCnttCapable(item.cnttCapable());
        capability.setTtkqCapable(item.ttkqCapable());
        capability.setPcrtCapable(item.pcrtCapable());
        capability.setTtqtCapable(item.ttqtCapable());
        capability.setXdcbCapable(item.xdcbCapable());
        capability.setTdCapable(item.tdCapable());
        capability.setTruongDoanCapable(item.truongDoanCapable());
        capability.setTruongNhomCapable(item.truongNhomCapable());
        capability.setToGiamSatCapable(item.toGiamSatCapable());
        capability.setDgclCapable(item.dgclCapable());
    }

    private Employee getOwnedEmployeeOrThrow(UUID tenantId, UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay nhan vien", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, String> usernamesByEmployeeId(List<Employee> employees) {
        if (employees.isEmpty()) {
            return Map.of();
        }
        return userAccountRepository.findByEmployeeIdIn(employees.stream().map(Employee::getId).toList()).stream()
                .collect(Collectors.toMap(UserAccount::getEmployeeId, UserAccount::getUsername, (a, b) -> a));
    }

    private Map<UUID, AuditEmployeeCapability> capabilitiesByEmployeeId(UUID tenantId) {
        return repository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(AuditEmployeeCapability::getEmployeeId, c -> c));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean parseCheckbox(String value) {
        if (isBlank(value)) {
            return false;
        }
        String normalized = value.trim().toUpperCase();
        return normalized.equals("X") || normalized.equals("TRUE") || normalized.equals("1");
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("username", "User Name"),
                new ExportColumn("fullName", "Tên cán bộ"),
                new ExportColumn("theCapable", "Thẻ"),
                new ExportColumn("qtdhCapable", "QTĐH"),
                new ExportColumn("hdvCapable", "HĐV"),
                new ExportColumn("tcktCapable", "TCKT"),
                new ExportColumn("cnttCapable", "CNTT"),
                new ExportColumn("ttkqCapable", "TTKQ"),
                new ExportColumn("pcrtCapable", "PCRT"),
                new ExportColumn("ttqtCapable", "TTQT"),
                new ExportColumn("xdcbCapable", "XDCB"),
                new ExportColumn("tdCapable", "TD"),
                new ExportColumn("truongDoanCapable", "Trưởng đoàn"),
                new ExportColumn("truongNhomCapable", "Trưởng nhóm"),
                new ExportColumn("toGiamSatCapable", "Tổ giám sát"),
                new ExportColumn("dgclCapable", "Thực hiện ĐGCL"),
                new ExportColumn("enteredBy", "User nhập"),
                new ExportColumn("approved", "Phê duyệt"),
                new ExportColumn("approvedBy", "User phê duyệt"),
                new ExportColumn("approvedAt", "Ngày phê duyệt"),
                new ExportColumn("updatedAt", "Ngày update"));
    }

    private List<Map<String, Object>> exportRows() {
        return list().stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("username", r.username());
            row.put("fullName", r.fullName());
            row.put("theCapable", checkboxLabel(r.theCapable()));
            row.put("qtdhCapable", checkboxLabel(r.qtdhCapable()));
            row.put("hdvCapable", checkboxLabel(r.hdvCapable()));
            row.put("tcktCapable", checkboxLabel(r.tcktCapable()));
            row.put("cnttCapable", checkboxLabel(r.cnttCapable()));
            row.put("ttkqCapable", checkboxLabel(r.ttkqCapable()));
            row.put("pcrtCapable", checkboxLabel(r.pcrtCapable()));
            row.put("ttqtCapable", checkboxLabel(r.ttqtCapable()));
            row.put("xdcbCapable", checkboxLabel(r.xdcbCapable()));
            row.put("tdCapable", checkboxLabel(r.tdCapable()));
            row.put("truongDoanCapable", checkboxLabel(r.truongDoanCapable()));
            row.put("truongNhomCapable", checkboxLabel(r.truongNhomCapable()));
            row.put("toGiamSatCapable", checkboxLabel(r.toGiamSatCapable()));
            row.put("dgclCapable", checkboxLabel(r.dgclCapable()));
            row.put("enteredBy", r.enteredBy());
            row.put("approved", checkboxLabel(r.approved()));
            row.put("approvedBy", r.approvedBy());
            row.put("approvedAt", r.approvedAt());
            row.put("updatedAt", r.updatedAt());
            return row;
        }).toList();
    }

    private String checkboxLabel(boolean value) {
        return value ? "X" : "";
    }

    private AuditEmployeeCapabilityResponse toResponse(Employee employee, AuditEmployeeCapability c, String username) {
        if (c == null) {
            return new AuditEmployeeCapabilityResponse(employee.getId(), employee.getEmployeeCode(), username, employee.getFullName(),
                    false, false, false, false, false, false, false, false, false, false, false, false, false, false,
                    null, null, false, null, null);
        }
        return new AuditEmployeeCapabilityResponse(employee.getId(), employee.getEmployeeCode(), username, employee.getFullName(),
                c.isTheCapable(), c.isQtdhCapable(), c.isHdvCapable(), c.isTcktCapable(), c.isCnttCapable(), c.isTtkqCapable(),
                c.isPcrtCapable(), c.isTtqtCapable(), c.isXdcbCapable(), c.isTdCapable(), c.isTruongDoanCapable(), c.isTruongNhomCapable(),
                c.isToGiamSatCapable(), c.isDgclCapable(), c.getCreatedBy(), c.getUpdatedAt(), c.isApproved(), c.getApprovedBy(), c.getApprovedAt());
    }
}
