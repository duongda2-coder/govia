package com.govia.audit.branchstaff.service;

import com.govia.audit.branchstaff.dto.AuditBranchStaffRequest;
import com.govia.audit.branchstaff.dto.AuditBranchStaffResponse;
import com.govia.audit.branchstaff.entity.AuditBranchStaff;
import com.govia.audit.branchstaff.repository.AuditBranchStaffRepository;
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
import java.util.Optional;
import java.util.UUID;

/** CRUD + Import/Export cho danh muc "Chuc danh can bo chi nhanh" (sheet ZTC_CN_NV). */
@Service
public class AuditBranchStaffService {

    private final AuditBranchStaffRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditBranchStaffService(AuditBranchStaffRepository repository, AuditLogService auditLogService,
                                    ExcelExportService excelExportService, WordExportService wordExportService,
                                    ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditBranchStaffResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByBranchCodeAscStaffNameAsc(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditBranchStaffResponse create(AuditBranchStaffRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.staffName(), null);

        AuditBranchStaff item = new AuditBranchStaff();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditBranchStaff", item.getId(), AuditAction.CREATE,
                "Tao chuc danh can bo chi nhanh: " + item.getBranchCode() + " - " + item.getStaffName());
        return toResponse(item);
    }

    @Transactional
    public AuditBranchStaffResponse update(UUID id, AuditBranchStaffRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditBranchStaff item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.staffName(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditBranchStaff", item.getId(), AuditAction.UPDATE,
                "Cap nhat chuc danh can bo chi nhanh: " + item.getBranchCode() + " - " + item.getStaffName());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditBranchStaff item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditBranchStaff", id, AuditAction.DELETE,
                "Xoa chuc danh can bo chi nhanh: " + item.getBranchCode() + " - " + item.getStaffName());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_branch_staff", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Chức danh cán bộ chi nhánh", exportColumns(), exportRows());
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
        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String branchCode = row.get("branchCode");
                String staffName = row.get("staffName");
                if (isBlank(branchCode) || isBlank(staffName)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh hoac Ten nhan vien");
                }

                Optional<AuditBranchStaff> existing = repository.findByTenantIdAndBranchCodeAndStaffName(tenantId, branchCode.trim(), staffName.trim());
                AuditBranchStaffRequest request = new AuditBranchStaffRequest(branchCode.trim(), staffName.trim(),
                        emptyToNull(row.get("position")), parseInt(row.get("priority")), emptyToNull(row.get("note")),
                        existing.map(AuditBranchStaff::isActive).orElse(true));
                if (existing.isPresent()) {
                    update(existing.get().getId(), request);
                } else {
                    create(request);
                }
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditBranchStaff", null, AuditAction.CREATE,
                "Import Excel chuc danh can bo chi nhanh: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditBranchStaff item, AuditBranchStaffRequest request) {
        item.setBranchCode(request.branchCode());
        item.setStaffName(request.staffName());
        item.setPosition(request.position());
        item.setPriority(request.priority());
        item.setNote(request.note());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, String staffName, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndStaffName(tenantId, branchCode, staffName)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_BRANCH_STAFF_DUPLICATE", "Nhan vien da ton tai tren chi nhanh nay: " + staffName);
                });
    }

    private AuditBranchStaff getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_BRANCH_STAFF_NOT_FOUND", "Khong tim thay chuc danh can bo chi nhanh", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("staffName", "Tên nhân viên"),
                new ExportColumn("position", "Chức vụ"),
                new ExportColumn("priority", "Ưu tiên"),
                new ExportColumn("note", "Ghi chú"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByBranchCodeAscStaffNameAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("branchCode", item.getBranchCode());
                    row.put("staffName", item.getStaffName());
                    row.put("position", item.getPosition());
                    row.put("priority", item.getPriority());
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

    private Integer parseInt(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AuditBranchStaffResponse toResponse(AuditBranchStaff item) {
        return new AuditBranchStaffResponse(item.getId(), item.getBranchCode(), item.getStaffName(),
                item.getPosition(), item.getPriority(), item.getNote(), item.isActive());
    }
}
