package com.govia.identity.controller;

import com.govia.core.export.ImportResult;
import com.govia.core.web.ApiResponse;
import com.govia.identity.dto.RolePermissionsRequest;
import com.govia.identity.dto.RoleRequest;
import com.govia.identity.dto.RoleResponse;
import com.govia.identity.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Quan ly Vai tro (Role) va gan quyen (Permission) cho tung role.
 * Chi SUPER_ADMIN duoc dung man hinh nay - day la cau hinh phan quyen cho toan he thong.
 */
@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.ok(roleService.list());
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] content = roleService.exportRolesExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"roles.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping
    public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        return ApiResponse.ok(roleService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleResponse> update(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return ApiResponse.ok(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        roleService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/permissions")
    public ApiResponse<List<String>> getPermissions(@PathVariable UUID id) {
        return ApiResponse.ok(roleService.getPermissionCodes(id));
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<Void> setPermissions(@PathVariable UUID id, @RequestBody RolePermissionsRequest request) {
        roleService.setPermissionCodes(id, request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/permissions/export")
    public ResponseEntity<byte[]> exportPermissions(@PathVariable UUID id) {
        byte[] content = roleService.exportPermissionsExcel(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"role-permissions.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping("/{id}/permissions/import")
    public ApiResponse<ImportResult> importPermissions(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(roleService.importPermissionsExcel(id, file));
    }
}
