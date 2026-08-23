package com.govia.audit.masterdata.controller;

import com.govia.audit.masterdata.dto.MasterDataCategoryInfo;
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

/** Man hinh "Danh muc" dung chung cho module Kiem toan noi bo - xem AuditMasterDataCategory de biet
 * danh sach day du va cach them/bot 1 loai danh muc moi. */
@RestController
@RequestMapping("/api/audit/master-data")
public class MasterDataItemController {

    private final MasterDataItemService service;

    public MasterDataItemController(MasterDataItemService service) {
        this.service = service;
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('PERM_AUDIT.MASTER_DATA.VIEW')")
    public ApiResponse<List<MasterDataCategoryInfo>> categories() {
        return ApiResponse.ok(service.listCategories());
    }

    @GetMapping("/{category}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.MASTER_DATA.VIEW')")
    public ApiResponse<List<MasterDataItemResponse>> list(@PathVariable AuditMasterDataCategory category) {
        return ApiResponse.ok(service.list(category));
    }

    @PostMapping("/{category}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.MASTER_DATA.CREATE')")
    public ApiResponse<MasterDataItemResponse> create(@PathVariable AuditMasterDataCategory category,
                                                        @Valid @RequestBody MasterDataItemRequest request) {
        return ApiResponse.ok(service.create(category, request));
    }

    @PutMapping("/{category}/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.MASTER_DATA.EDIT')")
    public ApiResponse<MasterDataItemResponse> update(@PathVariable AuditMasterDataCategory category, @PathVariable UUID id,
                                                        @Valid @RequestBody MasterDataItemRequest request) {
        return ApiResponse.ok(service.update(category, id, request));
    }

    @DeleteMapping("/{category}/{id}")
    @PreAuthorize("hasAuthority('PERM_AUDIT.MASTER_DATA.DELETE')")
    public ApiResponse<Void> delete(@PathVariable AuditMasterDataCategory category, @PathVariable UUID id) {
        service.delete(category, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{category}/export/excel")
    @PreAuthorize("hasAuthority('PERM_AUDIT.MASTER_DATA.EXPORT')")
    public ResponseEntity<byte[]> exportExcel(@PathVariable AuditMasterDataCategory category) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + category.name().toLowerCase() + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel(category));
    }

    @GetMapping("/{category}/export/word")
    @PreAuthorize("hasAuthority('PERM_AUDIT.MASTER_DATA.EXPORT')")
    public ResponseEntity<byte[]> exportWord(@PathVariable AuditMasterDataCategory category) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + category.name().toLowerCase() + ".docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord(category));
    }

    @PostMapping("/{category}/import")
    @PreAuthorize("hasAuthority('PERM_AUDIT.MASTER_DATA.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@PathVariable AuditMasterDataCategory category,
                                                   @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.importFromExcel(category, file));
    }
}
