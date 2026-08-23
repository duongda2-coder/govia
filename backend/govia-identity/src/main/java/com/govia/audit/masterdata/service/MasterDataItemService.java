package com.govia.audit.masterdata.service;

import com.govia.audit.masterdata.dto.MasterDataCategoryInfo;
import com.govia.audit.masterdata.dto.MasterDataItemRequest;
import com.govia.audit.masterdata.dto.MasterDataItemResponse;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
import com.govia.core.export.WordExportService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD + Import/Export dung CHUNG cho toan bo danh muc cua module Kiem toan noi bo (xem
 * AuditMasterDataCategory) - 1 service duy nhat thay vi viet lai cho tung danh muc, vi tat ca deu
 * cung hinh dang du lieu (ma/ten/mo ta/thu tu/hieu luc).
 */
@Service
public class MasterDataItemService {

    private final AuditMasterDataItemRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public MasterDataItemService(AuditMasterDataItemRepository repository, AuditLogService auditLogService,
                                  ExcelExportService excelExportService, WordExportService wordExportService,
                                  ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    public List<MasterDataCategoryInfo> listCategories() {
        return java.util.Arrays.stream(AuditMasterDataCategory.values())
                .map(c -> new MasterDataCategoryInfo(c.name(), c.getLabel(), c.getGroup().name(), c.getGroup().getLabel()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MasterDataItemResponse> list(AuditMasterDataCategory category) {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, category).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MasterDataItemResponse create(AuditMasterDataCategory category, MasterDataItemRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, category, request.code(), null);
        validateParent(tenantId, request.parentId());

        AuditMasterDataItem item = new AuditMasterDataItem();
        item.setTenantId(tenantId);
        item.setCategory(category);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditMasterDataItem", item.getId(), AuditAction.CREATE,
                "Tao danh muc " + category + ": " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public MasterDataItemResponse update(AuditMasterDataCategory category, UUID id, MasterDataItemRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditMasterDataItem item = getOwnedOrThrow(tenantId, category, id);
        checkNoDuplicateCode(tenantId, category, request.code(), id);
        validateParent(tenantId, request.parentId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditMasterDataItem", item.getId(), AuditAction.UPDATE,
                "Cap nhat danh muc " + category + ": " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public void delete(AuditMasterDataCategory category, UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditMasterDataItem item = getOwnedOrThrow(tenantId, category, id);
        repository.delete(item);
        auditLogService.record("AuditMasterDataItem", id, AuditAction.DELETE,
                "Xoa danh muc " + category + ": " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(AuditMasterDataCategory category) {
        return excelExportService.export(category.name(), exportColumns(), exportRows(category));
    }

    @Transactional(readOnly = true)
    public byte[] exportWord(AuditMasterDataCategory category) {
        return wordExportService.export(category.getLabel(), exportColumns(), exportRows(category));
    }

    @Transactional
    public ImportResult importFromExcel(AuditMasterDataCategory category, MultipartFile file) {
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
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma hoac Ten");
                }
                create(category, new MasterDataItemRequest(code.trim(), name.trim(),
                        emptyToNull(row.get("description")), null, parseDate(row.get("validFrom")),
                        parseDate(row.get("validTo")), parseInt(row.get("sortOrder")), true));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditMasterDataItem", null, AuditAction.CREATE,
                "Import Excel danh muc " + category + ": " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditMasterDataItem item, MasterDataItemRequest request) {
        item.setCode(request.code());
        item.setName(request.name());
        item.setDescription(request.description());
        item.setParentId(request.parentId());
        item.setValidFrom(request.validFrom());
        item.setValidTo(request.validTo());
        item.setSortOrder(request.sortOrder());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, AuditMasterDataCategory category, String code, UUID excludingId) {
        repository.findByTenantIdAndCategoryAndCode(tenantId, category, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("MASTER_DATA_CODE_DUPLICATE", "Ma da ton tai trong danh muc nay: " + code);
                });
    }

    private void validateParent(UUID tenantId, UUID parentId) {
        if (parentId == null) {
            return;
        }
        repository.findById(parentId)
                .filter(p -> p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("MASTER_DATA_PARENT_NOT_FOUND", "Danh muc cha khong ton tai"));
    }

    private AuditMasterDataItem getOwnedOrThrow(UUID tenantId, AuditMasterDataCategory category, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId) && item.getCategory() == category)
                .orElseThrow(() -> new BusinessException("MASTER_DATA_ITEM_NOT_FOUND", "Khong tim thay danh muc",
                        HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("code", "Ma"),
                new ExportColumn("name", "Ten"),
                new ExportColumn("description", "Mo ta"),
                new ExportColumn("validFrom", "Hieu luc tu"),
                new ExportColumn("validTo", "Hieu luc den"),
                new ExportColumn("sortOrder", "Thu tu"));
    }

    private List<Map<String, Object>> exportRows(AuditMasterDataCategory category) {
        return repository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(TenantContext.getTenantId(), category).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("description", item.getDescription());
                    row.put("validFrom", item.getValidFrom());
                    row.put("validTo", item.getValidTo());
                    row.put("sortOrder", item.getSortOrder());
                    return row;
                }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private MasterDataItemResponse toResponse(AuditMasterDataItem item) {
        return new MasterDataItemResponse(item.getId(), item.getCategory().name(), item.getCode(), item.getName(),
                item.getDescription(), item.getParentId(), item.getValidFrom(), item.getValidTo(),
                item.getSortOrder(), item.isActive());
    }
}
