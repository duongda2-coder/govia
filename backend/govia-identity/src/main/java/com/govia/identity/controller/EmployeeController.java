package com.govia.identity.controller;

import com.govia.core.export.ImportResult;
import com.govia.core.web.ApiResponse;
import com.govia.identity.dto.AdminResetPasswordRequest;
import com.govia.identity.dto.CreateUserAccountRequest;
import com.govia.identity.dto.EmployeeFilter;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.dto.EmployeeStatusRequest;
import com.govia.identity.service.EmployeeService;
import com.govia.identity.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final UserAccountService userAccountService;

    public EmployeeController(EmployeeService employeeService, UserAccountService userAccountService) {
        this.employeeService = employeeService;
        this.userAccountService = userAccountService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PEOPLE.EMPLOYEE.VIEW')")
    public ApiResponse<Page<EmployeeResponse>> list(@ModelAttribute EmployeeFilter filter, Pageable pageable) {
        return ApiResponse.ok(employeeService.list(filter, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.EMPLOYEE.VIEW')")
    public ApiResponse<EmployeeResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(employeeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PEOPLE.EMPLOYEE.CREATE')")
    public ApiResponse<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.ok(employeeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.EMPLOYEE.EDIT')")
    public ApiResponse<EmployeeResponse> update(@PathVariable UUID id, @Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.ok(employeeService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.EMPLOYEE.EDIT')")
    public ApiResponse<EmployeeResponse> changeStatus(@PathVariable UUID id,
                                                        @Valid @RequestBody EmployeeStatusRequest request) {
        return ApiResponse.ok(employeeService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.EMPLOYEE.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        employeeService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/account")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.EMPLOYEE.EDIT')")
    public ApiResponse<Void> createAccount(@PathVariable UUID id, @Valid @RequestBody CreateUserAccountRequest request) {
        userAccountService.createForEmployee(id, request);
        return ApiResponse.ok(null);
    }

    /** Chi SUPER_ADMIN duoc dat lai mat khau ho nhan vien khac (nhan vien quen mat khau, khong tu doi duoc qua /api/auth/password). */
    @PatchMapping("/{id}/account/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> resetAccountPassword(@PathVariable UUID id, @Valid @RequestBody AdminResetPasswordRequest request) {
        userAccountService.resetPassword(id, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.EMPLOYEE.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(employeeService.importFromExcel(file));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.EMPLOYEE.EXPORT')")
    public ResponseEntity<byte[]> exportExcel(@ModelAttribute EmployeeFilter filter) {
        byte[] content = employeeService.exportExcel(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.EMPLOYEE.EXPORT')")
    public ResponseEntity<byte[]> exportWord(@ModelAttribute EmployeeFilter filter) {
        byte[] content = employeeService.exportWord(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(content);
    }
}
