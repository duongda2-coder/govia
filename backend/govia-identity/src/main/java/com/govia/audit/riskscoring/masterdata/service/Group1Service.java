package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.Group1Request;
import com.govia.audit.riskscoring.masterdata.dto.Group1Response;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import com.govia.audit.riskscoring.masterdata.entity.RiskGroup1;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectCategoryRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskGroup1Repository;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** CRUD + Import/Export cho danh muc "Nhom chi tieu cap 1" (sheet ZTC_DGRR_Group1). */
@Service
public class Group1Service {

    private final RiskGroup1Repository repository;
    private final AuditObjectCategoryRepository auditObjectCategoryRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public Group1Service(RiskGroup1Repository repository, AuditObjectCategoryRepository auditObjectCategoryRepository,
                          AuditLogService auditLogService, ExcelExportService excelExportService,
                          WordExportService wordExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditObjectCategoryRepository = auditObjectCategoryRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<Group1Response> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditObjectCategory> categories = auditObjectCategoriesById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream().map(item -> toResponse(item, categories)).toList();
    }

    @Transactional
    public Group1Response create(Group1Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.auditObjectCategoryId(), request.code(), null);
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());

        RiskGroup1 item = new RiskGroup1();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskGroup1", item.getId(), AuditAction.CREATE, "Tao nhom chi tieu cap 1: " + item.getCode());
        return toResponse(item, auditObjectCategoriesById(tenantId));
    }

    @Transactional
    public Group1Response update(UUID id, Group1Request request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskGroup1 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.auditObjectCategoryId(), request.code(), id);
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskGroup1", item.getId(), AuditAction.UPDATE, "Cap nhat nhom chi tieu cap 1: " + item.getCode());
        return toResponse(item, auditObjectCategoriesById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskGroup1 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskGroup1", id, AuditAction.DELETE, "Xoa nhom chi tieu cap 1: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_group1", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Nhóm chỉ tiêu cấp 1", exportColumns(), exportRows());
    }

    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), exportColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        UUID tenantId = TenantContext.getTenantId();
        Map<String, UUID> categoryIdsByCode = new HashMap<>();
        auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> categoryIdsByCode.put(c.getCode(), c.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String auditObjectCategoryCode = row.get("auditObjectCategoryCode");
                String code = row.get("code");
                String name = row.get("name");
                if (isBlank(auditObjectCategoryCode) || isBlank(code) || isBlank(name)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Loai doi tuong kiem toan, Ma hoac Ten");
                }
                UUID auditObjectCategoryId = categoryIdsByCode.get(auditObjectCategoryCode.trim());
                if (auditObjectCategoryId == null) {
                    throw new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan: " + auditObjectCategoryCode);
                }
                create(new Group1Request(auditObjectCategoryId,
                        code.trim(), name.trim(), parseDecimal(row.get("weight")), emptyToNull(row.get("businessLineCode")),
                        parseDate(row.get("validFrom")), parseDate(row.get("validTo")), true));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskGroup1", null, AuditAction.CREATE,
                "Import Excel nhom chi tieu cap 1: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(RiskGroup1 item, Group1Request request) {
        item.setAuditObjectCategoryId(request.auditObjectCategoryId());
        item.setCode(request.code());
        item.setName(request.name());
        item.setWeight(request.weight());
        item.setBusinessLineCode(request.businessLineCode());
        item.setValidFrom(request.validFrom());
        item.setValidTo(request.validTo());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, UUID auditObjectCategoryId, String code, UUID excludingId) {
        repository.findByTenantIdAndAuditObjectCategoryIdAndCode(tenantId, auditObjectCategoryId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_GROUP1_CODE_DUPLICATE", "Ma nhom da ton tai: " + code);
                });
    }

    private void validateAuditObjectCategory(UUID tenantId, UUID auditObjectCategoryId) {
        auditObjectCategoryRepository.findById(auditObjectCategoryId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan"));
    }

    private RiskGroup1 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_GROUP1_NOT_FOUND", "Khong tim thay nhom chi tieu cap 1", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditObjectCategory> auditObjectCategoriesById(UUID tenantId) {
        Map<UUID, AuditObjectCategory> map = new HashMap<>();
        for (AuditObjectCategory c : auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("auditObjectCategoryCode", "Loai doi tuong kiem toan"),
                new ExportColumn("code", "Ma nhom"),
                new ExportColumn("name", "Ten nhom"),
                new ExportColumn("weight", "Trong so"),
                new ExportColumn("businessLineCode", "Ma nghiep vu"),
                new ExportColumn("validFrom", "Hieu luc tu"),
                new ExportColumn("validTo", "Hieu luc den"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditObjectCategory> categories = auditObjectCategoriesById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> {
                    AuditObjectCategory category = categories.get(item.getAuditObjectCategoryId());
                    Map<String, Object> row = new HashMap<>();
                    row.put("auditObjectCategoryCode", category != null ? category.getCode() : null);
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("weight", item.getWeight());
                    row.put("businessLineCode", item.getBusinessLineCode());
                    row.put("validFrom", item.getValidFrom());
                    row.put("validTo", item.getValidTo());
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

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Group1Response toResponse(RiskGroup1 item, Map<UUID, AuditObjectCategory> categories) {
        AuditObjectCategory category = categories.get(item.getAuditObjectCategoryId());
        return new Group1Response(item.getId(), item.getAuditObjectCategoryId(),
                category != null ? category.getCode() : null, category != null ? category.getName() : null,
                item.getCode(), item.getName(), item.getWeight(), item.getBusinessLineCode(),
                item.getValidFrom(), item.getValidTo(), item.isActive());
    }
}
