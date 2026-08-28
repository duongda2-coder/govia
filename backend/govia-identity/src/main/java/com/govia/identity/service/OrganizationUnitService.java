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
import com.govia.identity.dto.OrgUnitTreeNode;
import com.govia.identity.dto.OrganizationUnitRequest;
import com.govia.identity.dto.OrganizationUnitResponse;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.OrganizationUnit;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.OrganizationUnitRepository;
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

/**
 * CRUD cay to chuc (Khoi/Trung tam/Phong ban/Bo phan) qua parent_id + level_code.
 * Quy uoc level_code: 001 = Khoi, 002 = Trung tam, 003 = Phong ban, 004 = Bo phan.
 */
@Service
public class OrganizationUnitService {

    private static final Set<String> ALLOWED_LEVEL_CODES = Set.of("001", "002", "003", "004");

    private final OrganizationUnitRepository repository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public OrganizationUnitService(OrganizationUnitRepository repository,
                                    EmployeeRepository employeeRepository,
                                    AuditLogService auditLogService,
                                    ExcelExportService excelExportService,
                                    WordExportService wordExportService,
                                    ExcelImportService excelImportService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationUnitResponse> list() {
        List<OrganizationUnit> units = repository.findByTenantId(TenantContext.getTenantId());
        Map<UUID, String> employeeNames = employeeNameMap(units);
        return units.stream().map(u -> toResponse(u, employeeNames)).toList();
    }

    @Transactional(readOnly = true)
    public OrganizationUnitResponse getById(UUID id) {
        OrganizationUnit unit = getOwnedOrThrow(TenantContext.getTenantId(), id);
        return toResponse(unit, employeeNameMap(List.of(unit)));
    }

    @Transactional(readOnly = true)
    public List<OrgUnitTreeNode> tree() {
        List<OrganizationUnit> units = repository.findByTenantId(TenantContext.getTenantId());
        // Khong dung Collectors.groupingBy vi no tu choi key null (don vi goc khong co cha)
        Map<UUID, List<OrganizationUnit>> byParent = new HashMap<>();
        for (OrganizationUnit unit : units) {
            byParent.computeIfAbsent(unit.getParentId(), k -> new ArrayList<>()).add(unit);
        }
        List<OrganizationUnit> roots = byParent.getOrDefault(null, List.of());
        return roots.stream().map(r -> buildNode(r, byParent)).toList();
    }

    @Transactional
    public OrganizationUnitResponse create(OrganizationUnitRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        validateLevelCode(request.levelCode());

        repository.findByTenantIdAndCode(tenantId, request.code()).ifPresent(u -> {
            throw new BusinessException("ORG_UNIT_CODE_DUPLICATE", "Ma don vi da ton tai: " + request.code());
        });
        resolveParent(tenantId, request.parentId(), null);
        validateManagerEmployee(tenantId, request.managerEmployeeId());

        OrganizationUnit unit = new OrganizationUnit();
        unit.setTenantId(tenantId);
        applyRequest(unit, request);
        unit.setActive(true);
        unit = repository.save(unit);

        auditLogService.record("OrganizationUnit", unit.getId(), AuditAction.CREATE, "Tao don vi " + unit.getCode());
        return toResponse(unit, employeeNameMap(List.of(unit)));
    }

    @Transactional
    public OrganizationUnitResponse update(UUID id, OrganizationUnitRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        OrganizationUnit unit = getOwnedOrThrow(tenantId, id);
        validateLevelCode(request.levelCode());

        repository.findByTenantIdAndCode(tenantId, request.code())
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> {
                    throw new BusinessException("ORG_UNIT_CODE_DUPLICATE", "Ma don vi da ton tai: " + request.code());
                });
        resolveParent(tenantId, request.parentId(), id);
        validateManagerEmployee(tenantId, request.managerEmployeeId());

        applyRequest(unit, request);
        unit = repository.save(unit);

        auditLogService.record("OrganizationUnit", unit.getId(), AuditAction.UPDATE, "Cap nhat don vi " + unit.getCode());
        return toResponse(unit, employeeNameMap(List.of(unit)));
    }

    @Transactional
    public OrganizationUnitResponse setActive(UUID id, boolean active) {
        UUID tenantId = TenantContext.getTenantId();
        OrganizationUnit unit = getOwnedOrThrow(tenantId, id);
        unit.setActive(active);
        unit = repository.save(unit);

        auditLogService.record("OrganizationUnit", unit.getId(),
                active ? AuditAction.UPDATE : AuditAction.DELETE,
                (active ? "Kich hoat" : "Vo hieu hoa") + " don vi " + unit.getCode());
        return toResponse(unit, employeeNameMap(List.of(unit)));
    }

    /**
     * Xoa cung: chi cho phep khi khong con don vi con nao (parentId tro toi don vi nay) va khong con
     * nhan vien nao truc thuoc - tranh du lieu mo coi/vo hieu ca 1 nhanh cay to chuc.
     */
    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        OrganizationUnit unit = getOwnedOrThrow(tenantId, id);

        if (repository.existsByParentId(id)) {
            throw new BusinessException("ORG_UNIT_HAS_CHILDREN",
                    "Khong the xoa: don vi nay dang la don vi cha cua 1 don vi khac");
        }
        if (employeeRepository.existsByOrgUnitId(id)) {
            throw new BusinessException("ORG_UNIT_HAS_EMPLOYEES",
                    "Khong the xoa: don vi nay dang co nhan vien truc thuoc");
        }

        repository.delete(unit);
        auditLogService.record("OrganizationUnit", id, AuditAction.DELETE, "Xoa don vi " + unit.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("OrgUnits", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Danh sach don vi to chuc", exportColumns(), exportRows());
    }

    /**
     * Import Excel theo dung mau da xuat: Ma don vi cha va Ma NV truong don vi tham chieu theo MA (khong phai UUID)
     * nen de doc/sua tren Excel - luu y don vi cha phai nam o dong TRUOC don vi con trong cung file.
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
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String code = row.get("code");
                String name = row.get("name");
                if (isBlank(code) || isBlank(name)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma hoac Ten don vi");
                }
                String levelCode = emptyToNull(row.get("levelCode"));
                UUID parentId = resolveParentIdByCode(tenantId, row.get("parentCode"));
                UUID managerId = resolveManagerIdByEmployeeCode(tenantId, row.get("managerEmployeeCode"));

                create(new OrganizationUnitRequest(code.trim(), name.trim(), null, levelCode, parentId, managerId));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("OrganizationUnit", null, AuditAction.CREATE,
                "Import Excel don vi to chuc: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private UUID resolveParentIdByCode(UUID tenantId, String parentCode) {
        if (isBlank(parentCode)) {
            return null;
        }
        return repository.findByTenantIdAndCode(tenantId, parentCode.trim())
                .orElseThrow(() -> new BusinessException("IMPORT_PARENT_NOT_FOUND",
                        "Khong tim thay don vi cha co ma: " + parentCode))
                .getId();
    }

    private UUID resolveManagerIdByEmployeeCode(UUID tenantId, String employeeCode) {
        if (isBlank(employeeCode)) {
            return null;
        }
        return employeeRepository.findByTenantIdAndEmployeeCode(tenantId, employeeCode.trim())
                .orElseThrow(() -> new BusinessException("IMPORT_MANAGER_NOT_FOUND",
                        "Khong tim thay nhan vien co ma: " + employeeCode))
                .getId();
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("code", "Ma"),
                new ExportColumn("name", "Ten don vi"),
                new ExportColumn("levelCode", "Cap bac (001=Khoi,002=Trung tam,003=Phong ban,004=Bo phan)"),
                new ExportColumn("parentCode", "Ma don vi cha"),
                new ExportColumn("managerEmployeeCode", "Ma NV truong don vi"));
    }

    private List<Map<String, Object>> exportRows() {
        List<OrganizationUnit> units = repository.findByTenantId(TenantContext.getTenantId());
        Map<UUID, String> codeById = units.stream().collect(Collectors.toMap(OrganizationUnit::getId, OrganizationUnit::getCode));
        Set<UUID> managerIds = units.stream().map(OrganizationUnit::getManagerEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> managerCodeById = managerIds.isEmpty() ? Map.of()
                : employeeRepository.findAllById(managerIds).stream().collect(Collectors.toMap(Employee::getId, Employee::getEmployeeCode));

        return units.stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("code", u.getCode());
            row.put("name", u.getName());
            row.put("levelCode", u.getLevelCode());
            row.put("parentCode", u.getParentId() == null ? "" : codeById.get(u.getParentId()));
            row.put("managerEmployeeCode", u.getManagerEmployeeId() == null ? "" : managerCodeById.get(u.getManagerEmployeeId()));
            return row;
        }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private OrgUnitTreeNode buildNode(OrganizationUnit unit, Map<UUID, List<OrganizationUnit>> byParent) {
        List<OrgUnitTreeNode> children = byParent.getOrDefault(unit.getId(), List.of()).stream()
                .map(c -> buildNode(c, byParent))
                .toList();
        return new OrgUnitTreeNode(unit.getId(), unit.getCode(), unit.getName(), unit.getType(),
                unit.getLevelCode(), unit.isActive(), children);
    }

    /** Validate don vi cha thuoc cung tenant, va (khi sua) khong tao vong lap cha-con. */
    private void resolveParent(UUID tenantId, UUID parentId, UUID excludeId) {
        if (parentId == null) {
            return;
        }
        if (excludeId != null) {
            UUID cursor = parentId;
            Set<UUID> visited = new HashSet<>();
            while (cursor != null) {
                if (cursor.equals(excludeId)) {
                    throw new BusinessException("ORG_UNIT_CIRCULAR",
                            "Khong the chon don vi nay hoac don vi con cua no lam don vi cha (vong lap)");
                }
                if (!visited.add(cursor)) {
                    break;
                }
                UUID finalCursor = cursor;
                cursor = repository.findById(finalCursor).map(OrganizationUnit::getParentId).orElse(null);
            }
        }
        repository.findById(parentId)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("ORG_UNIT_PARENT_NOT_FOUND", "Don vi cha khong ton tai"));
    }

    private void validateLevelCode(String levelCode) {
        if (levelCode != null && !ALLOWED_LEVEL_CODES.contains(levelCode)) {
            throw new BusinessException("ORG_UNIT_INVALID_LEVEL_CODE",
                    "level_code khong hop le, chi chap nhan: " + ALLOWED_LEVEL_CODES);
        }
    }

    private void validateManagerEmployee(UUID tenantId, UUID managerEmployeeId) {
        if (managerEmployeeId == null) {
            return;
        }
        employeeRepository.findById(managerEmployeeId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("ORG_UNIT_MANAGER_NOT_FOUND", "Nhan vien quan ly khong ton tai"));
    }

    private void applyRequest(OrganizationUnit unit, OrganizationUnitRequest request) {
        unit.setCode(request.code());
        unit.setName(request.name());
        unit.setType(request.type());
        unit.setLevelCode(request.levelCode());
        unit.setParentId(request.parentId());
        unit.setManagerEmployeeId(request.managerEmployeeId());
    }

    private OrganizationUnit getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("ORG_UNIT_NOT_FOUND", "Khong tim thay don vi", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, String> employeeNameMap(List<OrganizationUnit> units) {
        Set<UUID> managerIds = units.stream()
                .map(OrganizationUnit::getManagerEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (managerIds.isEmpty()) {
            return Map.of();
        }
        return employeeRepository.findAllById(managerIds).stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getFullName));
    }

    private OrganizationUnitResponse toResponse(OrganizationUnit unit, Map<UUID, String> employeeNames) {
        String managerName = unit.getManagerEmployeeId() == null ? null : employeeNames.get(unit.getManagerEmployeeId());
        return new OrganizationUnitResponse(unit.getId(), unit.getCode(), unit.getName(), unit.getType(),
                unit.getLevelCode(), unit.getParentId(), unit.getManagerEmployeeId(), managerName, unit.isActive());
    }
}
