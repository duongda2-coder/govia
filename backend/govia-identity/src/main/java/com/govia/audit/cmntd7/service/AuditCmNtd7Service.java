package com.govia.audit.cmntd7.service;

import com.govia.audit.cmntd7.dto.AuditCmNtd7Request;
import com.govia.audit.cmntd7.dto.AuditCmNtd7Response;
import com.govia.audit.cmntd7.entity.AuditCmNtd7;
import com.govia.audit.cmntd7.repository.AuditCmNtd7Repository;
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

/** CRUD + Import/Export cho "Danh sach chon mau ho so cong trinh XDCB" (sheet ZTC_CM_NTD7). */
@Service
public class AuditCmNtd7Service {

    private final AuditCmNtd7Repository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmNtd7Service(AuditCmNtd7Repository repository, AuditLogService auditLogService,
                               ExcelExportService excelExportService, WordExportService wordExportService,
                               ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditCmNtd7Response> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditCmNtd7Response create(AuditCmNtd7Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.constructionCode(), null);

        AuditCmNtd7 item = new AuditCmNtd7();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd7", item.getId(), AuditAction.CREATE,
                "Tao ban ghi chon mau ho so cong trinh XDCB: " + item.getBranchCode() + " - " + item.getConstructionCode());
        return toResponse(item);
    }

    @Transactional
    public AuditCmNtd7Response update(UUID id, AuditCmNtd7Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd7 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.constructionCode(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd7", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi chon mau ho so cong trinh XDCB: " + item.getBranchCode() + " - " + item.getConstructionCode());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd7 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmNtd7", id, AuditAction.DELETE,
                "Xoa ban ghi chon mau ho so cong trinh XDCB: " + item.getBranchCode() + " - " + item.getConstructionCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_cm_ntd7", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Danh sách chọn mẫu hồ sơ công trình XDCB", exportColumns(), exportRows());
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
                String constructionCode = row.get("constructionCode");
                if (isBlank(branchCode) || isBlank(constructionCode)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh hoac Ma CT");
                }

                Optional<AuditCmNtd7> existing = repository.findByTenantIdAndBranchCodeAndConstructionCode(
                        tenantId, branchCode.trim(), constructionCode.trim());
                AuditCmNtd7Request request = new AuditCmNtd7Request(branchCode.trim(), constructionCode.trim(),
                        emptyToNull(row.get("constructionName")), emptyToNull(row.get("content")), emptyToNull(row.get("documentType")),
                        emptyToNull(row.get("completenessAssessment")), emptyToNull(row.get("assessment")), emptyToNull(row.get("auditResult")),
                        existing.map(AuditCmNtd7::isActive).orElse(true));
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

        auditLogService.record("AuditCmNtd7", null, AuditAction.CREATE,
                "Import Excel chon mau ho so cong trinh XDCB: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditCmNtd7 item, AuditCmNtd7Request request) {
        item.setBranchCode(request.branchCode());
        item.setConstructionCode(request.constructionCode());
        item.setConstructionName(request.constructionName());
        item.setContent(request.content());
        item.setDocumentType(request.documentType());
        item.setCompletenessAssessment(request.completenessAssessment());
        item.setAssessment(request.assessment());
        item.setAuditResult(request.auditResult());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, String constructionCode, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndConstructionCode(tenantId, branchCode, constructionCode)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_NTD7_DUPLICATE", "Ban ghi nay da ton tai: " + constructionCode);
                });
    }

    private AuditCmNtd7 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_NTD7_NOT_FOUND", "Khong tim thay ban ghi chon mau ho so cong trinh XDCB", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("constructionCode", "Mã CT"),
                new ExportColumn("constructionName", "Tên Công trình"),
                new ExportColumn("content", "Nội dung"),
                new ExportColumn("documentType", "Dạng tài liệu"),
                new ExportColumn("completenessAssessment", "Đánh giá tính đầy đủ"),
                new ExportColumn("assessment", "Đánh giá"),
                new ExportColumn("auditResult", "Kết quả kiểm toán"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("branchCode", item.getBranchCode());
                    row.put("constructionCode", item.getConstructionCode());
                    row.put("constructionName", item.getConstructionName());
                    row.put("content", item.getContent());
                    row.put("documentType", item.getDocumentType());
                    row.put("completenessAssessment", item.getCompletenessAssessment());
                    row.put("assessment", item.getAssessment());
                    row.put("auditResult", item.getAuditResult());
                    return row;
                }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private AuditCmNtd7Response toResponse(AuditCmNtd7 item) {
        return new AuditCmNtd7Response(item.getId(), item.getBranchCode(), item.getConstructionCode(), item.getConstructionName(),
                item.getContent(), item.getDocumentType(), item.getCompletenessAssessment(), item.getAssessment(),
                item.getAuditResult(), item.isActive());
    }
}
