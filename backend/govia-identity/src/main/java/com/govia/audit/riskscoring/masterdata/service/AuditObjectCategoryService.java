package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.AuditObjectCategoryRequest;
import com.govia.audit.riskscoring.masterdata.dto.AuditObjectCategoryResponse;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectCategoryRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD + Import/Export cho danh muc "Loai doi tuong kiem toan" (sheet ZTC_Loai_Dtkt) - danh muc goc,
 * la cha cua Group1 (xem AuditObjectCategoryRequest.auditObjectCategoryId tren RiskGroup1).
 */
@Service
public class AuditObjectCategoryService {

    private final AuditObjectCategoryRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditObjectCategoryService(AuditObjectCategoryRepository repository, AuditLogService auditLogService,
                                       ExcelExportService excelExportService, WordExportService wordExportService,
                                       ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditObjectCategoryResponse> list() {
        return repository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditObjectCategoryResponse create(AuditObjectCategoryRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);

        AuditObjectCategory item = new AuditObjectCategory();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditObjectCategory", item.getId(), AuditAction.CREATE, "Tao loai doi tuong kiem toan: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public AuditObjectCategoryResponse update(UUID id, AuditObjectCategoryRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectCategory item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditObjectCategory", item.getId(), AuditAction.UPDATE, "Cap nhat loai doi tuong kiem toan: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectCategory item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditObjectCategory", id, AuditAction.DELETE, "Xoa loai doi tuong kiem toan: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_audit_object_category", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Loại đối tượng kiểm toán", exportColumns(), exportRows());
    }

    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
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
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma hoac Ten loai doi tuong");
                }
                create(new AuditObjectCategoryRequest(code.trim(), name.trim(), emptyToNull(row.get("note")), true));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditObjectCategory", null, AuditAction.CREATE,
                "Import Excel loai doi tuong kiem toan: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditObjectCategory item, AuditObjectCategoryRequest request) {
        item.setCode(request.code());
        item.setName(request.name());
        item.setNote(request.note());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_OBJECT_CATEGORY_CODE_DUPLICATE", "Ma loai doi tuong da ton tai: " + code);
                });
    }

    private AuditObjectCategory getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("code", "Ma loai doi tuong"),
                new ExportColumn("name", "Ten loai doi tuong"),
                new ExportColumn("note", "Ghi chu"));
    }

    private List<Map<String, Object>> exportRows() {
        return repository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("note", item.getNote());
                    return row;
                }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private AuditObjectCategoryResponse toResponse(AuditObjectCategory item) {
        return new AuditObjectCategoryResponse(item.getId(), item.getCode(), item.getName(), item.getNote(), item.isActive());
    }
}
