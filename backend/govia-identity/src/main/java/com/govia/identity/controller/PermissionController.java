package com.govia.identity.controller;

import com.govia.core.web.ApiResponse;
import com.govia.identity.dto.PermissionResponse;
import com.govia.identity.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Danh muc quyen toan platform - dung de ve ma tran phan quyen tren man hinh Vai tro. Chi SUPER_ADMIN duoc xem. */
@RestController
@RequestMapping("/api/permissions")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PermissionController {

    private final RoleService roleService;

    public PermissionController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<PermissionResponse>> list() {
        return ApiResponse.ok(roleService.listPermissions());
    }
}
