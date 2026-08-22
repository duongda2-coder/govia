package com.govia.identity.controller;

import com.govia.core.web.ApiResponse;
import com.govia.identity.dto.AccountSummaryResponse;
import com.govia.identity.dto.AssignRolesRequest;
import com.govia.identity.dto.CopyRolesRequest;
import com.govia.identity.service.UserAccountService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Quan ly tai khoan dang nhap + gan vai tro. Chi SUPER_ADMIN duoc xem/gan quyen cho tai khoan khac. */
@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AccountController {

    private final UserAccountService userAccountService;

    public AccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public ApiResponse<List<AccountSummaryResponse>> list() {
        return ApiResponse.ok(userAccountService.listAccounts());
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] content = userAccountService.exportAccountsExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"accounts.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable UUID id, @RequestBody AssignRolesRequest request) {
        userAccountService.assignRoles(id, request);
        return ApiResponse.ok(null);
    }

    /** Sao chep toan bo vai tro (nen quyen) tu 1 tai khoan khac sang tai khoan {id} - ghi de vai tro hien co. */
    @PostMapping("/{id}/copy-roles")
    public ApiResponse<Void> copyRoles(@PathVariable UUID id, @RequestBody CopyRolesRequest request) {
        userAccountService.copyRoles(id, request.sourceAccountId());
        return ApiResponse.ok(null);
    }
}
