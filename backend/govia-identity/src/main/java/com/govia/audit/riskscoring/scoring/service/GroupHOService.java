package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.scoring.dto.GroupHORequest;
import com.govia.audit.riskscoring.scoring.dto.GroupHOResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskGroupHO;
import com.govia.audit.riskscoring.scoring.repository.RiskGroupHORepository;
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

/** CRUD + Import/Export cho danh muc "Nhom rui ro HO theo tuyen bao ve" (sheet ZTC_Nhom_DGRR_HO). */
@Service
public class GroupHOService {

    private final RiskGroupHORepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public GroupHOService(RiskGroupHORepository repository, AuditLogService auditLogService,
                           ExcelExportService excelExportService, WordExportService wordExportService,
                           ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<GroupHOResponse> list() {
        return repository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public GroupHOResponse create(GroupHORequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);

        RiskGroupHO item = new RiskGroupHO();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskGroupHO", item.getId(), AuditAction.CREATE, "Tao nhom rui ro HO: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public GroupHOResponse update(UUID id, GroupHORequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskGroupHO item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskGroupHO", item.getId(), AuditAction.UPDATE, "Cap nhat nhom rui ro HO: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskGroupHO item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskGroupHO", id, AuditAction.DELETE, "Xoa nhom rui ro HO: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_group_ho", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Nhóm rủi ro HO theo tuyến bảo vệ", exportColumns(), exportRows());
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
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma nhom hoac Ten nhom");
                }
                create(new GroupHORequest(code.trim(), name.trim(), emptyToNull(row.get("note")), true));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskGroupHO", null, AuditAction.CREATE,
                "Import Excel nhom rui ro HO: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(RiskGroupHO item, GroupHORequest request) {
        item.setCode(request.code());
        item.setName(request.name());
        item.setNote(request.note());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_GROUP_HO_CODE_DUPLICATE", "Ma nhom da ton tai: " + code);
                });
    }

    private RiskGroupHO getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_GROUP_HO_NOT_FOUND", "Khong tim thay nhom rui ro HO", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("code", "Ma nhom"),
                new ExportColumn("name", "Ten nhom"),
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

    private GroupHOResponse toResponse(RiskGroupHO item) {
        return new GroupHOResponse(item.getId(), item.getCode(), item.getName(), item.getNote(), item.isActive());
    }
}
