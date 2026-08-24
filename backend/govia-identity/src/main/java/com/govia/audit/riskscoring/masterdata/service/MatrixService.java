package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.MatrixRequest;
import com.govia.audit.riskscoring.masterdata.dto.MatrixResponse;
import com.govia.audit.riskscoring.masterdata.entity.RiskMatrix;
import com.govia.audit.riskscoring.masterdata.repository.RiskMatrixRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** CRUD + Import/Export cho danh muc "Ma tran quy doi diem rui ro" (sheet ztc_mtrr_dt). */
@Service
public class MatrixService {

    private final RiskMatrixRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public MatrixService(RiskMatrixRepository repository, AuditLogService auditLogService,
                          ExcelExportService excelExportService, WordExportService wordExportService,
                          ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<MatrixResponse> list() {
        return repository.findByTenantIdOrderByFrequencyLevelAsc(TenantContext.getTenantId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public MatrixResponse create(MatrixRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.frequencyLevel(), null);

        RiskMatrix item = new RiskMatrix();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskMatrix", item.getId(), AuditAction.CREATE, "Tao dong ma tran rui ro muc tan suat " + item.getFrequencyLevel());
        return toResponse(item);
    }

    @Transactional
    public MatrixResponse update(UUID id, MatrixRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskMatrix item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.frequencyLevel(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskMatrix", item.getId(), AuditAction.UPDATE, "Cap nhat ma tran rui ro muc tan suat " + item.getFrequencyLevel());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskMatrix item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskMatrix", id, AuditAction.DELETE, "Xoa dong ma tran rui ro muc tan suat " + item.getFrequencyLevel());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_matrix", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Ma trận điểm rủi ro", exportColumns(), exportRows());
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
                Integer frequencyLevel = parseInt(row.get("frequencyLevel"));
                String frequencyLabel = row.get("frequencyLabel");
                if (frequencyLevel == null || isBlank(frequencyLabel)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Muc tan suat hoac Ten muc tan suat");
                }
                create(new MatrixRequest(frequencyLevel, frequencyLabel.trim(), parseDecimal(row.get("scoreLowSeverity")),
                        parseDecimal(row.get("scoreMediumSeverity")), parseDecimal(row.get("scoreHighSeverity")), true));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskMatrix", null, AuditAction.CREATE,
                "Import Excel ma tran rui ro: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(RiskMatrix item, MatrixRequest request) {
        item.setFrequencyLevel(request.frequencyLevel());
        item.setFrequencyLabel(request.frequencyLabel());
        item.setScoreLowSeverity(request.scoreLowSeverity());
        item.setScoreMediumSeverity(request.scoreMediumSeverity());
        item.setScoreHighSeverity(request.scoreHighSeverity());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, Integer frequencyLevel, UUID excludingId) {
        repository.findByTenantIdAndFrequencyLevel(tenantId, frequencyLevel)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_MATRIX_DUPLICATE", "Da ton tai dong ma tran cho muc tan suat " + frequencyLevel);
                });
    }

    private RiskMatrix getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_MATRIX_NOT_FOUND", "Khong tim thay dong ma tran rui ro", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("frequencyLevel", "Muc tan suat"),
                new ExportColumn("frequencyLabel", "Ten muc tan suat"),
                new ExportColumn("scoreLowSeverity", "Diem - RR thap"),
                new ExportColumn("scoreMediumSeverity", "Diem - RR trung binh"),
                new ExportColumn("scoreHighSeverity", "Diem - RR cao"));
    }

    private List<Map<String, Object>> exportRows() {
        return repository.findByTenantIdOrderByFrequencyLevelAsc(TenantContext.getTenantId()).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("frequencyLevel", item.getFrequencyLevel());
                    row.put("frequencyLabel", item.getFrequencyLabel());
                    row.put("scoreLowSeverity", item.getScoreLowSeverity());
                    row.put("scoreMediumSeverity", item.getScoreMediumSeverity());
                    row.put("scoreHighSeverity", item.getScoreHighSeverity());
                    return row;
                }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    private MatrixResponse toResponse(RiskMatrix item) {
        return new MatrixResponse(item.getId(), item.getFrequencyLevel(), item.getFrequencyLabel(),
                item.getScoreLowSeverity(), item.getScoreMediumSeverity(), item.getScoreHighSeverity(), item.isActive());
    }
}
