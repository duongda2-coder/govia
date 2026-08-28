package com.govia.identity.controller;

import com.govia.audit.masterdata.dto.MasterDataItemRequest;
import com.govia.audit.masterdata.dto.MasterDataItemResponse;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.service.MasterDataItemService;
import com.govia.core.export.ImportResult;
import com.govia.core.web.ApiResponse;
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

/** Man hinh "Danh muc Chuc vu" trong Nhan su - du lieu dung chung MasterDataItemService/bang
 * audit_master_data_item (category=POSITION) voi cac danh muc Kiem toan noi bo khac, nhung dat
 * permission RIENG PEOPLE.POSITION.* (khong dung chung AUDIT.MASTER_DATA.*) vi day la danh muc
 * thuoc module Nhan su - thay the han "Chuc danh" (bang position rieng) da bi xoa. */
@RestController
@RequestMapping("/api/people/positions")
public class PositionCatalogController {

    private static final AuditMasterDataCategory CATEGORY = AuditMasterDataCategory.POSITION;

    private final MasterDataItemService service;

    public PositionCatalogController(MasterDataItemService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.VIEW')")
    public ApiResponse<List<MasterDataItemResponse>> list() {
        return ApiResponse.ok(service.list(CATEGORY));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.CREATE')")
    public ApiResponse<MasterDataItemResponse> create(@Valid @RequestBody MasterDataItemRequest request) {
        return ApiResponse.ok(service.create(CATEGORY, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.EDIT')")
    public ApiResponse<MasterDataItemResponse> update(@PathVariable UUID id, @Valid @RequestBody MasterDataItemRequest request) {
        return ApiResponse.ok(service.update(CATEGORY, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.DELETE')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(CATEGORY, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"positions.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel(CATEGORY));
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"positions.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord(CATEGORY));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(CATEGORY, file));
    }
}
