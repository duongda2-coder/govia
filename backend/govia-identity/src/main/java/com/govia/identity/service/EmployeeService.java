package com.govia.identity.service;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
import com.govia.core.export.WordExportService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.identity.dto.EmployeeFilter;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.EmployeeStatus;
import com.govia.identity.entity.OrganizationUnit;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.OrganizationUnitRepository;
import com.govia.identity.repository.UserAccountRepository;
import com.govia.identity.service.spec.EmployeeSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository repository;
    private final OrganizationUnitRepository orgUnitRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;
    private final EmployeeApprovalService employeeApprovalService;
    private final UserAccountService userAccountService;

    public EmployeeService(EmployeeRepository repository,
                            OrganizationUnitRepository orgUnitRepository,
                            UserAccountRepository userAccountRepository,
                            AuditMasterDataItemRepository masterDataItemRepository,
                            AuditLogService auditLogService,
                            ExcelExportService excelExportService,
                            WordExportService wordExportService,
                            ExcelImportService excelImportService,
                            EmployeeApprovalService employeeApprovalService,
                            UserAccountService userAccountService) {
        this.repository = repository;
        this.orgUnitRepository = orgUnitRepository;
        this.userAccountRepository = userAccountRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
        this.employeeApprovalService = employeeApprovalService;
        this.userAccountService = userAccountService;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(EmployeeFilter filter, Pageable pageable) {
        Page<Employee> page = repository.findAll(buildSpec(filter), pageable);
        ResponseContext ctx = buildResponseContext(page.getContent());
        return page.map(e -> toResponse(e, ctx));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        Employee employee = getOwnedOrThrow(TenantContext.getTenantId(), id);
        return toResponse(employee, buildResponseContext(List.of(employee)));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        UUID tenantId = TenantContext.getTenantId();

        repository.findByTenantIdAndEmployeeCode(tenantId, request.employeeCode()).ifPresent(e -> {
            throw new BusinessException("EMPLOYEE_CODE_DUPLICATE", "Ma nhan vien da ton tai: " + request.employeeCode());
        });
        validateOrgUnit(tenantId, request.orgUnitId());
        validatePosition(tenantId, request.positionId());
        validateManagerExists(tenantId, request.managerId());
        validateBusinessSegment(tenantId, request.businessSegmentId());

        Employee employee = new Employee();
        employee.setTenantId(tenantId);
        applyRequest(employee, request);
        /*
         * Nhan vien co quan ly truc tiep (managerId) se cho duyet qua quy trinh Flowable
         * "employee_approval" (di nguoc chuoi quan ly toi da 3 cap + buoc cuoi Super Admin) truoc khi
         * chuyen ACTIVE - xem EmployeeApprovalService. Khong co quan ly (vd nhan vien dau tien cua 1
         * don vi) thi ACTIVE ngay, khong can duyet.
         */
        employee.setStatus(request.managerId() != null ? EmployeeStatus.PENDING_APPROVAL : EmployeeStatus.ACTIVE);
        employee = repository.save(employee);

        auditLogService.record("Employee", employee.getId(), AuditAction.CREATE, "Tao nhan vien " + employee.getEmployeeCode());

        if (employee.getManagerId() != null) {
            employeeApprovalService.startApprovalIfNeeded(employee);
        }

        return toResponse(employee, buildResponseContext(List.of(employee)));
    }

    @Transactional
    public EmployeeResponse update(UUID id, EmployeeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Employee employee = getOwnedOrThrow(tenantId, id);

        repository.findByTenantIdAndEmployeeCode(tenantId, request.employeeCode())
                .filter(e -> !e.getId().equals(id))
                .ifPresent(e -> {
                    throw new BusinessException("EMPLOYEE_CODE_DUPLICATE", "Ma nhan vien da ton tai: " + request.employeeCode());
                });
        validateOrgUnit(tenantId, request.orgUnitId());
        validatePosition(tenantId, request.positionId());
        validateManager(tenantId, id, request.managerId());
        validateBusinessSegment(tenantId, request.businessSegmentId());

        applyRequest(employee, request);
        employee = repository.save(employee);

        auditLogService.record("Employee", employee.getId(), AuditAction.UPDATE, "Cap nhat nhan vien " + employee.getEmployeeCode());
        return toResponse(employee, buildResponseContext(List.of(employee)));
    }

    @Transactional
    public EmployeeResponse changeStatus(UUID id, EmployeeStatus status) {
        UUID tenantId = TenantContext.getTenantId();
        Employee employee = getOwnedOrThrow(tenantId, id);
        employee.setStatus(status);
        employee = repository.save(employee);

        auditLogService.record("Employee", employee.getId(), AuditAction.UPDATE,
                "Doi trang thai nhan vien " + employee.getEmployeeCode() + " sang " + status);
        return toResponse(employee, buildResponseContext(List.of(employee)));
    }

    /**
     * Xoa cung: chi cho phep khi khong con gi tham chieu toi nhan vien nay (khong phai quan ly cua ai,
     * khong gan voi user_account nao, khong dang la truong don vi to chuc nao) - tranh du lieu mo coi/loi FK.
     */
    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Employee employee = getOwnedOrThrow(tenantId, id);

        if (repository.existsByManagerId(id)) {
            throw new BusinessException("EMPLOYEE_HAS_SUBORDINATES",
                    "Khong the xoa: nhan vien nay dang la quan ly truc tiep cua nguoi khac");
        }
        if (userAccountRepository.existsByEmployeeId(id)) {
            throw new BusinessException("EMPLOYEE_HAS_USER_ACCOUNT",
                    "Khong the xoa: nhan vien nay dang gan voi 1 tai khoan dang nhap");
        }
        if (orgUnitRepository.existsByManagerEmployeeId(id)) {
            throw new BusinessException("EMPLOYEE_IS_ORG_UNIT_MANAGER",
                    "Khong the xoa: nhan vien nay dang la truong cua 1 don vi to chuc");
        }

        repository.delete(employee);
        auditLogService.record("Employee", id, AuditAction.DELETE, "Xoa nhan vien " + employee.getEmployeeCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(EmployeeFilter filter) {
        List<Employee> employees = repository.findAll(buildSpec(filter));
        auditLogService.record("Employee", null, AuditAction.EXPORT,
                "Xuat Excel danh sach nhan vien (" + employees.size() + " dong)");
        return excelExportService.export("Employees", exportColumns(), exportRows(employees));
    }

    @Transactional(readOnly = true)
    public byte[] exportWord(EmployeeFilter filter) {
        List<Employee> employees = repository.findAll(buildSpec(filter));
        auditLogService.record("Employee", null, AuditAction.EXPORT,
                "Xuat Word danh sach nhan vien (" + employees.size() + " dong)");
        return wordExportService.export("Danh sach nhan vien", exportColumns(), exportRows(employees));
    }

    /**
     * Import Excel theo dung mau da xuat (exportColumns): Don vi/Chuc danh tham chieu theo TEN
     * (khop khong phan biet hoa thuong voi du lieu da co san), Trang thai ghi dung ma enum
     * (ACTIVE/ON_LEAVE/TERMINATED), bo trong se mac dinh ACTIVE. Tung dong loi duoc bao rieng,
     * khong lam hong ca file. Cot "Ten dang nhap" (neu co gia tri) se duoc dung de TAO LUON tai
     * khoan dang nhap cho nhan vien do, voi mat khau ngau nhien - vi file import (dung mau xuat)
     * khong co cot mat khau nen khong the tu dat mat khau nguoi dung. Mat khau tam duoc tra ve
     * trong "notices" cua ImportResult de admin gui lai cho nhan vien doi. Neu tao tai khoan that
     * bai (vd trung ten dang nhap) thi nhan vien VAN duoc tao, chi rieng dong do bao loi tai khoan.
     */
    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        UUID tenantId = TenantContext.getTenantId();
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), exportColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        List<String> notices = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String employeeCode = row.get("employeeCode");
                String fullName = row.get("fullName");
                if (isBlank(employeeCode) || isBlank(fullName)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma NV hoac Ho ten");
                }
                UUID orgUnitId = resolveOrgUnitIdByName(tenantId, row.get("orgUnitName"));
                UUID positionId = resolvePositionIdByName(tenantId, row.get("positionName"));
                EmployeeStatus status = resolveStatus(row.get("status"));

                EmployeeRequest request = new EmployeeRequest(employeeCode.trim(), fullName.trim(), null, null,
                        emptyToNull(row.get("phone")), orgUnitId, positionId, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null);
                EmployeeResponse created = create(request);
                if (status != EmployeeStatus.ACTIVE) {
                    changeStatus(created.id(), status);
                }

                String username = emptyToNull(row.get("username"));
                if (username != null) {
                    try {
                        String tempPassword = userAccountService.createForEmployeeWithGeneratedPassword(created.id(), username);
                        notices.add("Dong " + rowNumber + ": da tao tai khoan dang nhap \"" + username
                                + "\" cho " + employeeCode.trim() + " - mat khau tam: " + tempPassword);
                    } catch (BusinessException e) {
                        errors.add(new ImportResult.ImportRowError(rowNumber,
                                "Da tao nhan vien nhung khong tao duoc tai khoan dang nhap: " + e.getMessage()));
                    }
                }
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("Employee", null, AuditAction.CREATE,
                "Import Excel nhan vien: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors, notices);
    }

    private UUID resolveOrgUnitIdByName(UUID tenantId, String name) {
        if (isBlank(name)) {
            return null;
        }
        return orgUnitRepository.findByTenantIdAndNameIgnoreCase(tenantId, name.trim())
                .orElseThrow(() -> new BusinessException("IMPORT_ORG_UNIT_NOT_FOUND", "Khong tim thay don vi ten: " + name))
                .getId();
    }

    private UUID resolvePositionIdByName(UUID tenantId, String name) {
        if (isBlank(name)) {
            return null;
        }
        return masterDataItemRepository.findByTenantIdAndCategoryAndNameIgnoreCase(tenantId, AuditMasterDataCategory.POSITION, name.trim())
                .orElseThrow(() -> new BusinessException("IMPORT_POSITION_NOT_FOUND", "Khong tim thay chuc vu ten: " + name))
                .getId();
    }

    private EmployeeStatus resolveStatus(String value) {
        if (isBlank(value)) {
            return EmployeeStatus.ACTIVE;
        }
        try {
            return EmployeeStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("IMPORT_INVALID_STATUS", "Trang thai khong hop le: " + value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private Specification<Employee> buildSpec(EmployeeFilter filter) {
        return Specification.where(EmployeeSpecifications.tenantId(TenantContext.getTenantId()))
                .and(EmployeeSpecifications.orgUnitId(filter.orgUnitId()))
                .and(EmployeeSpecifications.status(filter.status()))
                .and(EmployeeSpecifications.keyword(filter.keyword()))
                .and(EmployeeSpecifications.fieldContains("employeeCode", filter.employeeCode()))
                .and(EmployeeSpecifications.fieldContains("fullName", filter.fullName()))
                .and(EmployeeSpecifications.fieldContains("phone", filter.phone()))
                .and(EmployeeSpecifications.fieldContains("email", filter.email()))
                .and(EmployeeSpecifications.orgUnitNameContains(filter.orgUnitName()))
                .and(EmployeeSpecifications.positionNameContains(filter.positionName()))
                .and(EmployeeSpecifications.managerNameContains(filter.managerName()));
    }

    /** Day du TAT CA truong co trong form Them/Sua nhan vien (EmployeeFormDrawer) cong them
     * username/status (khong thuoc form nhung la thong tin nhan vien can xem) - TRU accountPassword
     * (mat khau tai khoan dang nhap, khong bao gio duoc xuat ra). */
    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("employeeCode", "Ma NV"),
                new ExportColumn("fullName", "Ho ten"),
                new ExportColumn("gender", "Gioi tinh"),
                new ExportColumn("email", "Email cong viec"),
                new ExportColumn("personalEmail", "Email ca nhan"),
                new ExportColumn("phone", "Dien thoai"),
                new ExportColumn("idNumber", "So CCCD/CMND"),
                new ExportColumn("orgUnitName", "Don vi"),
                new ExportColumn("positionName", "Chuc danh"),
                new ExportColumn("managerName", "Quan ly truc tiep"),
                new ExportColumn("hireDate", "Ngay vao lam"),
                new ExportColumn("dateOfBirth", "Ngay sinh"),
                new ExportColumn("rankLevel", "Cap bac"),
                new ExportColumn("ethnicity", "Dan toc"),
                new ExportColumn("businessSegmentName", "Linh vuc"),
                new ExportColumn("hometown", "Que quan"),
                new ExportColumn("partyJoinDate", "Ngay vao Dang"),
                new ExportColumn("auditDeptJoinDate", "Ngay vao Ban Kiem soat"),
                new ExportColumn("priorWorkHistory", "Qua trinh cong tac truoc Agribank"),
                new ExportColumn("educationLevel", "Trinh do chuyen mon"),
                new ExportColumn("politicalLevel", "Trinh do chinh tri"),
                new ExportColumn("foreignLanguageLevel", "Trinh do ngoai ngu"),
                new ExportColumn("itSkillLevel", "Trinh do tin hoc"),
                new ExportColumn("auditorClassification", "Phan loai nang luc KTV"),
                new ExportColumn("teamLeadCapable", "Kha nang truong doan"),
                new ExportColumn("auditedBranches", "Chi nhanh da kiem toan nam truoc"),
                new ExportColumn("otherDuties", "Cong tac khac"),
                new ExportColumn("relatedPersonBranches", "Chi nhanh co nguoi lien quan"),
                new ExportColumn("onLeave", "Dang nghi che do"),
                new ExportColumn("username", "Ten dang nhap"),
                new ExportColumn("status", "Trang thai"));
    }

    private List<Map<String, Object>> exportRows(List<Employee> employees) {
        ResponseContext ctx = buildResponseContext(employees);
        return employees.stream().map(e -> {
            Map<String, Object> row = new HashMap<>();
            row.put("employeeCode", e.getEmployeeCode());
            row.put("fullName", e.getFullName());
            row.put("gender", e.getGender());
            row.put("email", e.getEmail());
            row.put("personalEmail", e.getPersonalEmail());
            row.put("phone", e.getPhone());
            row.put("idNumber", e.getIdNumber());
            row.put("orgUnitName", e.getOrgUnitId() == null ? "" : nameOf(ctx.orgUnits.get(e.getOrgUnitId())));
            row.put("positionName", e.getPositionId() == null ? "" : nameOf(ctx.positions.get(e.getPositionId())));
            row.put("managerName", e.getManagerId() == null ? "" : nameOfEmployee(ctx.managers.get(e.getManagerId())));
            row.put("hireDate", e.getHireDate());
            row.put("dateOfBirth", e.getDateOfBirth());
            row.put("rankLevel", e.getRankLevel());
            row.put("ethnicity", e.getEthnicity());
            row.put("businessSegmentName", e.getBusinessSegmentId() == null ? "" : nameOf(ctx.businessSegments.get(e.getBusinessSegmentId())));
            row.put("hometown", e.getHometown());
            row.put("partyJoinDate", e.getPartyJoinDate());
            row.put("auditDeptJoinDate", e.getAuditDeptJoinDate());
            row.put("priorWorkHistory", e.getPriorWorkHistory());
            row.put("educationLevel", e.getEducationLevel());
            row.put("politicalLevel", e.getPoliticalLevel());
            row.put("foreignLanguageLevel", e.getForeignLanguageLevel());
            row.put("itSkillLevel", e.getItSkillLevel());
            row.put("auditorClassification", e.getAuditorClassification());
            row.put("teamLeadCapable", e.isTeamLeadCapable() ? "Y" : "N");
            row.put("auditedBranches", e.getAuditedBranches());
            row.put("otherDuties", e.getOtherDuties());
            row.put("relatedPersonBranches", e.getRelatedPersonBranches());
            row.put("onLeave", e.isOnLeave() ? "Y" : "N");
            row.put("username", ctx.usernames.get(e.getId()));
            row.put("status", e.getStatus());
            return row;
        }).toList();
    }

    private void validateOrgUnit(UUID tenantId, UUID orgUnitId) {
        if (orgUnitId == null) {
            return;
        }
        orgUnitRepository.findById(orgUnitId)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("ORG_UNIT_NOT_FOUND", "Don vi to chuc khong ton tai"));
    }

    /** "Chuc vu" phai la 1 dong danh muc POSITION that su (khong duoc tro sang danh muc khac cung bang audit_master_data_item). */
    private void validatePosition(UUID tenantId, UUID positionId) {
        if (positionId == null) {
            return;
        }
        masterDataItemRepository.findById(positionId)
                .filter(item -> item.getTenantId().equals(tenantId) && item.getCategory() == AuditMasterDataCategory.POSITION)
                .orElseThrow(() -> new BusinessException("POSITION_NOT_FOUND", "Chuc vu khong ton tai"));
    }

    /** "Linh vuc" phai la 1 dong danh muc BUSINESS_SEGMENT that su (khong duoc tro sang danh muc khac cung bang audit_master_data_item). */
    private void validateBusinessSegment(UUID tenantId, UUID businessSegmentId) {
        if (businessSegmentId == null) {
            return;
        }
        masterDataItemRepository.findById(businessSegmentId)
                .filter(item -> item.getTenantId().equals(tenantId) && item.getCategory() == AuditMasterDataCategory.BUSINESS_SEGMENT)
                .orElseThrow(() -> new BusinessException("BUSINESS_SEGMENT_NOT_FOUND", "Khong tim thay linh vuc/mang nghiep vu"));
    }

    private void validateManagerExists(UUID tenantId, UUID managerId) {
        if (managerId == null) {
            return;
        }
        repository.findById(managerId)
                .filter(m -> m.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_MANAGER_NOT_FOUND", "Quan ly khong ton tai"));
    }

    /** Khi sua: kiem tra quan ly khong phai chinh minh, va khong tao vong lap bao cao (A quan ly B, B quan ly A...). */
    private void validateManager(UUID tenantId, UUID employeeId, UUID managerId) {
        if (managerId == null) {
            return;
        }
        if (managerId.equals(employeeId)) {
            throw new BusinessException("EMPLOYEE_INVALID_MANAGER", "Nhan vien khong the la quan ly cua chinh minh");
        }
        Employee manager = repository.findById(managerId)
                .filter(m -> m.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_MANAGER_NOT_FOUND", "Quan ly khong ton tai"));

        Set<UUID> visited = new HashSet<>();
        UUID cursor = manager.getManagerId();
        while (cursor != null) {
            if (cursor.equals(employeeId)) {
                throw new BusinessException("EMPLOYEE_MANAGER_CIRCULAR",
                        "Vong lap quan ly: nhan vien khong the bao cao cho cap duoi cua chinh minh");
            }
            if (!visited.add(cursor)) {
                break;
            }
            UUID finalCursor = cursor;
            cursor = repository.findById(finalCursor).map(Employee::getManagerId).orElse(null);
        }
    }

    private void applyRequest(Employee employee, EmployeeRequest request) {
        employee.setEmployeeCode(request.employeeCode());
        employee.setFullName(request.fullName());
        employee.setEmail(request.email());
        employee.setPersonalEmail(request.personalEmail());
        employee.setPhone(request.phone());
        employee.setOrgUnitId(request.orgUnitId());
        employee.setPositionId(request.positionId());
        employee.setHireDate(request.hireDate());
        employee.setDateOfBirth(request.dateOfBirth());
        employee.setGender(request.gender());
        employee.setIdNumber(request.idNumber());
        employee.setManagerId(request.managerId());
        employee.setRankLevel(request.rankLevel());
        employee.setEthnicity(request.ethnicity());
        employee.setHometown(request.hometown());
        employee.setPartyJoinDate(request.partyJoinDate());
        employee.setAuditDeptJoinDate(request.auditDeptJoinDate());
        employee.setPriorWorkHistory(request.priorWorkHistory());
        employee.setEducationLevel(request.educationLevel());
        employee.setPoliticalLevel(request.politicalLevel());
        employee.setForeignLanguageLevel(request.foreignLanguageLevel());
        employee.setItSkillLevel(request.itSkillLevel());
        employee.setAuditorClassification(request.auditorClassification());
        employee.setTeamLeadCapable(request.teamLeadCapable());
        employee.setAuditedBranches(request.auditedBranches());
        employee.setOtherDuties(request.otherDuties());
        employee.setRelatedPersonBranches(request.relatedPersonBranches());
        employee.setOnLeave(request.onLeave());
        employee.setBusinessSegmentId(request.businessSegmentId());
    }

    private Employee getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("EMPLOYEE_NOT_FOUND", "Khong tim thay nhan vien", HttpStatus.NOT_FOUND));
    }

    /** Gom du lieu lien quan (don vi, chuc danh, quan ly, tai khoan) cho 1 lo Employee - tranh N+1 query. */
    private ResponseContext buildResponseContext(List<Employee> employees) {
        Set<UUID> orgUnitIds = employees.stream().map(Employee::getOrgUnitId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> positionIds = employees.stream().map(Employee::getPositionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> managerIds = employees.stream().map(Employee::getManagerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> employeeIds = employees.stream().map(Employee::getId).collect(Collectors.toSet());
        Set<UUID> businessSegmentIds = employees.stream().map(Employee::getBusinessSegmentId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<UUID, OrganizationUnit> orgUnits = orgUnitIds.isEmpty() ? Map.of()
                : orgUnitRepository.findAllById(orgUnitIds).stream().collect(Collectors.toMap(OrganizationUnit::getId, u -> u));
        Map<UUID, AuditMasterDataItem> positions = positionIds.isEmpty() ? Map.of()
                : masterDataItemRepository.findAllById(positionIds).stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
        Map<UUID, Employee> managers = managerIds.isEmpty() ? Map.of()
                : repository.findAllById(managerIds).stream().collect(Collectors.toMap(Employee::getId, m -> m));
        Map<UUID, String> usernames = employeeIds.isEmpty() ? Map.of()
                : userAccountRepository.findByEmployeeIdIn(employeeIds).stream()
                        .collect(Collectors.toMap(UserAccount::getEmployeeId, UserAccount::getUsername, (a, b) -> a));
        Map<UUID, AuditMasterDataItem> businessSegments = businessSegmentIds.isEmpty() ? Map.of()
                : masterDataItemRepository.findAllById(businessSegmentIds).stream()
                        .collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));

        return new ResponseContext(orgUnits, positions, managers, usernames, businessSegments);
    }

    private EmployeeResponse toResponse(Employee e, ResponseContext ctx) {
        OrganizationUnit orgUnit = e.getOrgUnitId() == null ? null : ctx.orgUnits.get(e.getOrgUnitId());
        AuditMasterDataItem position = e.getPositionId() == null ? null : ctx.positions.get(e.getPositionId());
        Employee manager = e.getManagerId() == null ? null : ctx.managers.get(e.getManagerId());
        AuditMasterDataItem businessSegment = e.getBusinessSegmentId() == null ? null : ctx.businessSegments.get(e.getBusinessSegmentId());

        return new EmployeeResponse(
                e.getId(), e.getEmployeeCode(), e.getFullName(), e.getEmail(), e.getPersonalEmail(), e.getPhone(),
                e.getOrgUnitId(), orgUnit == null ? null : orgUnit.getCode(), orgUnit == null ? null : orgUnit.getName(),
                e.getPositionId(), position == null ? null : position.getCode(), position == null ? null : position.getName(),
                e.getHireDate(), e.getStatus(), e.getDateOfBirth(), e.getGender(), e.getIdNumber(),
                e.getManagerId(), manager == null ? null : manager.getEmployeeCode(), manager == null ? null : manager.getFullName(),
                e.getRankLevel(),
                e.getEthnicity(), e.getHometown(), e.getPartyJoinDate(), e.getAuditDeptJoinDate(), e.getPriorWorkHistory(),
                e.getEducationLevel(), e.getPoliticalLevel(), e.getForeignLanguageLevel(), e.getItSkillLevel(),
                e.getAuditorClassification(), e.isTeamLeadCapable(), e.getAuditedBranches(), e.getOtherDuties(),
                e.getRelatedPersonBranches(), e.isOnLeave(),
                e.getBusinessSegmentId(), businessSegment == null ? null : businessSegment.getCode(), businessSegment == null ? null : businessSegment.getName(),
                ctx.usernames.get(e.getId()),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private static String nameOf(OrganizationUnit unit) {
        return unit == null ? "" : unit.getName();
    }

    private static String nameOf(AuditMasterDataItem item) {
        return item == null ? "" : item.getName();
    }

    private static String nameOfEmployee(Employee employee) {
        return employee == null ? "" : employee.getFullName();
    }

    private record ResponseContext(
            Map<UUID, OrganizationUnit> orgUnits,
            Map<UUID, AuditMasterDataItem> positions,
            Map<UUID, Employee> managers,
            Map<UUID, String> usernames,
            Map<UUID, AuditMasterDataItem> businessSegments
    ) {
    }
}
