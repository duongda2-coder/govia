package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.AuditObjectProcessRequest;
import com.govia.audit.riskscoring.masterdata.dto.AuditObjectProcessResponse;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectProcess;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectProcessRepository;
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

/** CRUD + Import/Export cho danh muc "Doi tuong kiem toan Quy trinh" (sheet ZTC_DTKT4). */
@Service
public class AuditObjectProcessService {

    private final AuditObjectProcessRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditObjectProcessService(AuditObjectProcessRepository repository, AuditLogService auditLogService,
                                      ExcelExportService excelExportService, WordExportService wordExportService,
                                      ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditObjectProcessResponse> list() {
        return repository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditObjectProcessResponse create(AuditObjectProcessRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);

        AuditObjectProcess item = new AuditObjectProcess();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditObjectProcess", item.getId(), AuditAction.CREATE, "Tao quy trinh: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public AuditObjectProcessResponse update(UUID id, AuditObjectProcessRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectProcess item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditObjectProcess", item.getId(), AuditAction.UPDATE, "Cap nhat quy trinh: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectProcess item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditObjectProcess", id, AuditAction.DELETE, "Xoa quy trinh: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_audit_object_process", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Đối tượng kiểm toán Quy trình", exportColumns(), exportRows());
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
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma quy trinh hoac Ten quy trinh");
                }
                create(new AuditObjectProcessRequest(emptyToNull(row.get("segmentCode")), code.trim(), name.trim(),
                        emptyToNull(row.get("referenceDocument")), emptyToNull(row.get("auditResult")),
                        emptyToNull(row.get("eventNote")), emptyToNull(row.get("incidentNote")),
                        emptyToNull(row.get("reviewResult")), true));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditObjectProcess", null, AuditAction.CREATE,
                "Import Excel quy trinh: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditObjectProcess item, AuditObjectProcessRequest request) {
        item.setSegmentCode(request.segmentCode());
        item.setCode(request.code());
        item.setName(request.name());
        item.setReferenceDocument(request.referenceDocument());
        item.setAuditResult(request.auditResult());
        item.setEventNote(request.eventNote());
        item.setIncidentNote(request.incidentNote());
        item.setReviewResult(request.reviewResult());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_OBJECT_PROCESS_CODE_DUPLICATE", "Ma quy trinh da ton tai: " + code);
                });
    }

    private AuditObjectProcess getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_PROCESS_NOT_FOUND", "Khong tim thay quy trinh", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("segmentCode", "Mang nghiep vu"),
                new ExportColumn("code", "Ma quy trinh"),
                new ExportColumn("name", "Quy trinh"),
                new ExportColumn("referenceDocument", "Van ban tham chieu"),
                new ExportColumn("auditResult", "Ket qua kiem toan"),
                new ExportColumn("eventNote", "Su kien"),
                new ExportColumn("incidentNote", "Vu viec"),
                new ExportColumn("reviewResult", "Ket qua ra soat"));
    }

    private List<Map<String, Object>> exportRows() {
        return repository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("segmentCode", item.getSegmentCode());
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("referenceDocument", item.getReferenceDocument());
                    row.put("auditResult", item.getAuditResult());
                    row.put("eventNote", item.getEventNote());
                    row.put("incidentNote", item.getIncidentNote());
                    row.put("reviewResult", item.getReviewResult());
                    return row;
                }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private AuditObjectProcessResponse toResponse(AuditObjectProcess item) {
        return new AuditObjectProcessResponse(item.getId(), item.getSegmentCode(), item.getCode(), item.getName(),
                item.getReferenceDocument(), item.getAuditResult(), item.getEventNote(), item.getIncidentNote(),
                item.getReviewResult(), item.isActive());
    }
}
