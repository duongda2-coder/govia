package com.govia.audit.employeecapability.controller;

import com.govia.audit.employeecapability.dto.AuditEmployeeCapabilityItemRequest;
import com.govia.audit.employeecapability.dto.AuditEmployeeCapabilityResponse;
import com.govia.audit.employeecapability.service.AuditEmployeeCapabilityService;
import com.govia.core.export.ImportResult;
import com.govia.core.web.ApiResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** Man hinh "Danh muc Khai bao kha nang dam nhan linh vuc cua nhan vien" (sheet ZTC_KNDN, xem AuditEmployeeCapabilityService). */
@RestController
@RequestMapping("/api/audit/master-data/employee-capability")
public class AuditEmployeeCapabilityController {

    private final AuditEmployeeCapabilityService service;

    public AuditEmployeeCapabilityController(AuditEmployeeCapabilityService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT.EMPLOYEE_CAPABILITY.VIEW')")
    public ApiResponse<List<AuditEmployeeCapabilityResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PutMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EMPLOYEE_CAPABILITY.EDIT')")
    public ApiResponse<List<AuditEmployeeCapabilityResponse>> bulkUpdate(@Valid @RequestBody List<AuditEmployeeCapabilityItemRequest> items) {
        return ApiResponse.ok(service.bulkUpdate(items));
    }

    @PostMapping("/{employeeId}/approve")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EMPLOYEE_CAPABILITY.EDIT')")
    public ApiResponse<AuditEmployeeCapabilityResponse> approve(@PathVariable UUID employeeId) {
        return ApiResponse.ok(service.approve(employeeId));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EMPLOYEE_CAPABILITY.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_employee_capability.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EMPLOYEE_CAPABILITY.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_employee_capability.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.EMPLOYEE_CAPABILITY.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(file));
    }
}
