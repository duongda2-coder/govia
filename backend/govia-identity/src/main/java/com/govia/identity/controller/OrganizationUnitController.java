package com.govia.identity.controller;

import com.govia.core.export.ImportResult;
import com.govia.core.web.ApiResponse;
import com.govia.identity.dto.OrgUnitActiveRequest;
import com.govia.identity.dto.OrgUnitTreeNode;
import com.govia.identity.dto.OrganizationUnitRequest;
import com.govia.identity.dto.OrganizationUnitResponse;
import com.govia.identity.service.OrganizationUnitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

@RestController
@RequestMapping("/api/org-units")
public class OrganizationUnitController {

    private final OrganizationUnitService organizationUnitService;

    public OrganizationUnitController(OrganizationUnitService organizationUnitService) {
        this.organizationUnitService = organizationUnitService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PEOPLE.ORGUNIT.VIEW')")
    public ApiResponse<List<OrganizationUnitResponse>> list() {
        return ApiResponse.ok(organizationUnitService.list());
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.ORGUNIT.VIEW')")
    public ApiResponse<List<OrgUnitTreeNode>> tree() {
        return ApiResponse.ok(organizationUnitService.tree());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.ORGUNIT.VIEW')")
    public ApiResponse<OrganizationUnitResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(organizationUnitService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PEOPLE.ORGUNIT.CREATE')")
    public ApiResponse<OrganizationUnitResponse> create(@Valid @RequestBody OrganizationUnitRequest request) {
        return ApiResponse.ok(organizationUnitService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.ORGUNIT.EDIT')")
    public ApiResponse<OrganizationUnitResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody OrganizationUnitRequest request) {
        return ApiResponse.ok(organizationUnitService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.ORGUNIT.EDIT')")
    public ApiResponse<OrganizationUnitResponse> setActive(@PathVariable UUID id,
                                                             @Valid @RequestBody OrgUnitActiveRequest request) {
        return ApiResponse.ok(organizationUnitService.setActive(id, request.active()));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.ORGUNIT.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"org-units.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(organizationUnitService.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.ORGUNIT.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"org-units.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(organizationUnitService.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.ORGUNIT.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(organizationUnitService.importFromExcel(file));
    }
}
