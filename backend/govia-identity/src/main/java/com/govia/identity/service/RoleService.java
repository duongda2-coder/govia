package com.govia.identity.service;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.PermissionResponse;
import com.govia.identity.dto.RolePermissionsRequest;
import com.govia.identity.dto.RoleRequest;
import com.govia.identity.dto.RoleResponse;
import com.govia.identity.entity.Permission;
import com.govia.identity.entity.Role;
import com.govia.identity.entity.RolePermission;
import com.govia.identity.repository.PermissionRepository;
import com.govia.identity.repository.RolePermissionRepository;
import com.govia.identity.repository.RoleRepository;
import com.govia.identity.repository.UserRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Quan ly Role (vai tro) va gan quyen (Permission) cho tung role - nen tang RBAC cho toan platform.
 * Chi SUPER_ADMIN duoc goi cac API nay (xem @PreAuthorize tren RoleController) vi day la cau hinh
 * phan quyen cho toan he thong, khong the/kh nen delegate cho role khac tu quan ly.
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    public RoleService(RoleRepository roleRepository,
                        PermissionRepository permissionRepository,
                        RolePermissionRepository rolePermissionRepository,
                        UserRoleRepository userRoleRepository,
                        AuditLogService auditLogService,
                        ExcelExportService excelExportService,
                        ExcelImportService excelImportService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
    }

    public List<RoleResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        return roleRepository.findByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportRolesExcel() {
        List<Map<String, Object>> rows = list().stream()
                .map(r -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("code", r.code());
                    row.put("name", r.name());
                    row.put("description", r.description());
                    row.put("systemDefined", r.systemDefined() ? "X" : "");
                    return row;
                })
                .toList();
        return excelExportService.export("Vai tro", roleExportColumns(), rows);
    }

    private List<ExportColumn> roleExportColumns() {
        return List.of(
                new ExportColumn("code", "Ma"),
                new ExportColumn("name", "Ten vai tro"),
                new ExportColumn("description", "Mo ta"),
                new ExportColumn("systemDefined", "Vai tro he thong (X)"));
    }

    /** Danh muc quyen toan platform, dung de ve ma tran phan quyen tren UI - loai bo quyen wildcard "*" (chi noi bo). */
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .filter(p -> !"*".equals(p.getCode()))
                .map(this::toPermissionResponse)
                .toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        roleRepository.findByTenantIdAndCode(tenantId, request.code()).ifPresent(r -> {
            throw new BusinessException("ROLE_CODE_DUPLICATE", "Ma vai tro da ton tai: " + request.code());
        });

        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setSystemDefined(false);
        role = roleRepository.save(role);

        auditLogService.record("Role", role.getId(), AuditAction.CREATE, "Tao vai tro \"" + role.getName() + "\"");
        return toResponse(role);
    }

    @Transactional
    public RoleResponse update(UUID id, RoleRequest request) {
        Role role = getOwnedRole(id);
        if (role.isSystemDefined()) {
            throw new BusinessException("ROLE_SYSTEM_DEFINED", "Khong the sua vai tro he thong");
        }
        if (!role.getCode().equals(request.code())) {
            roleRepository.findByTenantIdAndCode(role.getTenantId(), request.code()).ifPresent(r -> {
                throw new BusinessException("ROLE_CODE_DUPLICATE", "Ma vai tro da ton tai: " + request.code());
            });
        }
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        role = roleRepository.save(role);

        auditLogService.record("Role", role.getId(), AuditAction.UPDATE, "Cap nhat vai tro \"" + role.getName() + "\"");
        return toResponse(role);
    }

    @Transactional
    public void delete(UUID id) {
        Role role = getOwnedRole(id);
        if (role.isSystemDefined()) {
            throw new BusinessException("ROLE_SYSTEM_DEFINED", "Khong the xoa vai tro he thong");
        }
        if (userRoleRepository.existsByRoleId(id)) {
            throw new BusinessException("ROLE_IN_USE", "Vai tro dang duoc gan cho tai khoan, khong the xoa");
        }
        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.delete(role);

        auditLogService.record("Role", role.getId(), AuditAction.DELETE, "Xoa vai tro \"" + role.getName() + "\"");
    }

    public List<String> getPermissionCodes(UUID roleId) {
        getOwnedRole(roleId);
        List<UUID> permissionIds = rolePermissionRepository.findByRoleId(roleId).stream()
                .map(RolePermission::getPermissionId)
                .toList();
        return permissionRepository.findAllById(permissionIds).stream().map(Permission::getCode).toList();
    }

    @Transactional
    public void setPermissionCodes(UUID roleId, RolePermissionsRequest request) {
        Role role = getOwnedRole(roleId);
        if (role.isSystemDefined()) {
            throw new BusinessException("ROLE_SYSTEM_DEFINED", "Khong the sua quyen cua vai tro he thong");
        }

        List<String> codes = request.permissionCodes() == null ? List.of() : request.permissionCodes();
        List<Permission> permissions = permissionRepository.findByCodeIn(codes);

        rolePermissionRepository.deleteByRoleId(roleId);
        rolePermissionRepository.flush();
        for (Permission permission : permissions) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setTenantId(role.getTenantId());
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permission.getId());
            rolePermissionRepository.save(rolePermission);
        }

        auditLogService.record("Role", roleId, AuditAction.UPDATE,
                "Cap nhat quyen cho vai tro \"" + role.getName() + "\": " + permissions.size() + " quyen");
    }

    /**
     * Xuat ma tran phan quyen dang danh sach phang (khong phai ma tran cot theo action) - moi dong la 1 quyen,
     * cot "Duoc cap" danh dau X neu role dang co quyen do. Dang phang nay scale duoc voi hang tram man hinh vi
     * khong can them cot moi khi co man hinh/hanh dong moi, khac voi bang ma tran co dinh tren UI.
     */
    @Transactional(readOnly = true)
    public byte[] exportPermissionsExcel(UUID roleId) {
        Role role = getOwnedRole(roleId);
        Set<String> granted = new HashSet<>(getPermissionCodes(roleId));

        List<Map<String, Object>> rows = permissionRepository.findAll().stream()
                .filter(p -> !"*".equals(p.getCode()))
                .sorted(Comparator.comparing(Permission::getCode))
                .map(p -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("module", p.getModule());
                    row.put("resourceLabel", p.getResourceLabel());
                    row.put("code", p.getCode());
                    row.put("description", p.getDescription());
                    row.put("granted", granted.contains(p.getCode()) ? "X" : "");
                    return row;
                })
                .toList();

        return excelExportService.export("Phan quyen", permissionExportColumns(), rows);
    }

    /**
     * Import lai dung file da xuat: doc cot "Ma quyen" + "Duoc cap", dong nao co danh dau o cot Duoc cap
     * (bat ky ky tu nao, khong chi "X") thi quyen do duoc gan cho role - GHI DE toan bo quyen hien tai cua
     * role (giong hanh vi Luu tren UI), khong phai cong don.
     */
    @Transactional
    public ImportResult importPermissionsExcel(UUID roleId, MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), permissionExportColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        Set<String> catalogCodes = new HashSet<>(permissionRepository.findAll().stream().map(Permission::getCode).toList());

        Set<String> grantedCodes = new LinkedHashSet<>();
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            String granted = row.get("granted");
            if (granted == null || granted.isBlank()) {
                continue;
            }
            String code = row.get("code") == null ? "" : row.get("code").trim();
            if (code.isEmpty() || !catalogCodes.contains(code)) {
                errors.add(new ImportResult.ImportRowError(rowNumber, "Ma quyen khong hop le: " + code));
                continue;
            }
            grantedCodes.add(code);
        }

        setPermissionCodes(roleId, new RolePermissionsRequest(new ArrayList<>(grantedCodes)));
        return new ImportResult(grantedCodes.size(), errors.size(), errors);
    }

    private List<ExportColumn> permissionExportColumns() {
        return List.of(
                new ExportColumn("module", "Module"),
                new ExportColumn("resourceLabel", "Man hinh"),
                new ExportColumn("code", "Ma quyen"),
                new ExportColumn("description", "Mo ta"),
                new ExportColumn("granted", "Duoc cap (X)"));
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getCode(), permission.getModule(),
                permission.getDescription(), permission.getResourceLabel());
    }

    private Role getOwnedRole(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return roleRepository.findById(id)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Khong tim thay vai tro", HttpStatus.NOT_FOUND));
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDescription(), role.isSystemDefined());
    }
}
