package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.FrequencyCoefficientRequest;
import com.govia.audit.riskscoring.masterdata.dto.FrequencyCoefficientResponse;
import com.govia.audit.riskscoring.masterdata.entity.RiskFrequencyCoefficient;
import com.govia.audit.riskscoring.masterdata.repository.RiskFrequencyCoefficientRepository;
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

/** CRUD + Import/Export cho danh muc "He so tan suat xuat hien sai pham" (sheet ZTC_HSSP_DT). */
@Service
public class FrequencyCoefficientService {

    private final RiskFrequencyCoefficientRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public FrequencyCoefficientService(RiskFrequencyCoefficientRepository repository, AuditLogService auditLogService,
                                        ExcelExportService excelExportService, WordExportService wordExportService,
                                        ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<FrequencyCoefficientResponse> list() {
        return repository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public FrequencyCoefficientResponse create(FrequencyCoefficientRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);
        validateYearRange(request.fromYear(), request.toYear());

        RiskFrequencyCoefficient item = new RiskFrequencyCoefficient();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskFrequencyCoefficient", item.getId(), AuditAction.CREATE, "Tao he so tan suat: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public FrequencyCoefficientResponse update(UUID id, FrequencyCoefficientRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskFrequencyCoefficient item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);
        validateYearRange(request.fromYear(), request.toYear());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskFrequencyCoefficient", item.getId(), AuditAction.UPDATE, "Cap nhat he so tan suat: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskFrequencyCoefficient item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskFrequencyCoefficient", id, AuditAction.DELETE, "Xoa he so tan suat: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_frequency_coefficient", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Hệ số tần suất xuất hiện sai phạm", exportColumns(), exportRows());
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
                String label = row.get("label");
                if (isBlank(code) || isBlank(label)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma hoac Ten he so");
                }
                create(new FrequencyCoefficientRequest(code.trim(), parseInt(row.get("fromYear")), parseInt(row.get("toYear")),
                        label.trim(), parseDecimal(row.get("value")), parseDecimal(row.get("bonusPoint")),
                        "Y".equalsIgnoreCase(row.get("repeat")), row.get("repeatCount"), parseDecimal(row.get("repeatRiskPoint")), true));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskFrequencyCoefficient", null, AuditAction.CREATE,
                "Import Excel he so tan suat: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(RiskFrequencyCoefficient item, FrequencyCoefficientRequest request) {
        item.setCode(request.code());
        item.setFromYear(request.fromYear());
        item.setToYear(request.toYear());
        item.setLabel(request.label());
        item.setValue(request.value());
        item.setBonusPoint(request.bonusPoint());
        item.setRepeat(request.repeat());
        item.setRepeatCount(request.repeatCount());
        item.setRepeatRiskPoint(request.repeatRiskPoint());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_FREQ_COEF_CODE_DUPLICATE", "Ma he so da ton tai: " + code);
                });
    }

    private void validateYearRange(Integer fromYear, Integer toYear) {
        if (fromYear != null && toYear != null && fromYear > toYear) {
            throw new BusinessException("RISK_FREQ_COEF_YEAR_RANGE_INVALID", "Tu nam phai nho hon hoac bang Den nam");
        }
    }

    private RiskFrequencyCoefficient getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_FREQ_COEF_NOT_FOUND", "Khong tim thay he so tan suat", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("code", "Ma he so"),
                new ExportColumn("fromYear", "Tu nam"),
                new ExportColumn("toYear", "Den nam"),
                new ExportColumn("label", "Ten he so"),
                new ExportColumn("value", "Gia tri"),
                new ExportColumn("bonusPoint", "Diem cong"),
                new ExportColumn("repeat", "Lap lai"),
                new ExportColumn("repeatCount", "So lan lap lai"),
                new ExportColumn("repeatRiskPoint", "Diem RR lap lai"));
    }

    private List<Map<String, Object>> exportRows() {
        return repository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("code", item.getCode());
                    row.put("fromYear", item.getFromYear());
                    row.put("toYear", item.getToYear());
                    row.put("label", item.getLabel());
                    row.put("value", item.getValue());
                    row.put("bonusPoint", item.getBonusPoint());
                    row.put("repeat", item.isRepeat() ? "Y" : "N");
                    row.put("repeatCount", item.getRepeatCount());
                    row.put("repeatRiskPoint", item.getRepeatRiskPoint());
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

    private FrequencyCoefficientResponse toResponse(RiskFrequencyCoefficient item) {
        return new FrequencyCoefficientResponse(item.getId(), item.getCode(), item.getFromYear(), item.getToYear(),
                item.getLabel(), item.getValue(), item.getBonusPoint(), item.isRepeat(), item.getRepeatCount(),
                item.getRepeatRiskPoint(), item.isActive());
    }
}
