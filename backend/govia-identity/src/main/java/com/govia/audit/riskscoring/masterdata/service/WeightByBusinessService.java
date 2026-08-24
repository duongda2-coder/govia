package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.WeightByBusinessRequest;
import com.govia.audit.riskscoring.masterdata.dto.WeightByBusinessResponse;
import com.govia.audit.riskscoring.masterdata.entity.RiskWeightByBusiness;
import com.govia.audit.riskscoring.masterdata.repository.RiskWeightByBusinessRepository;
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

/** CRUD + Import/Export cho danh muc "Ty trong DT/DL theo Nghiep vu" (sheet ZTC_DLDT_TT). */
@Service
public class WeightByBusinessService {

    private final RiskWeightByBusinessRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public WeightByBusinessService(RiskWeightByBusinessRepository repository, AuditLogService auditLogService,
                                    ExcelExportService excelExportService, WordExportService wordExportService,
                                    ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<WeightByBusinessResponse> list() {
        return repository.findByTenantIdOrderByBusinessCodeAscFromYearAsc(TenantContext.getTenantId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public WeightByBusinessResponse create(WeightByBusinessRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.businessCode(), request.fromYear(), null);
        validateWeightSum(request.qualitativeWeight(), request.quantitativeWeight());
        validateYearRange(request.fromYear(), request.toYear());

        RiskWeightByBusiness item = new RiskWeightByBusiness();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskWeightByBusiness", item.getId(), AuditAction.CREATE, "Tao ty trong theo nghiep vu: " + item.getBusinessCode());
        return toResponse(item);
    }

    @Transactional
    public WeightByBusinessResponse update(UUID id, WeightByBusinessRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskWeightByBusiness item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.businessCode(), request.fromYear(), id);
        validateWeightSum(request.qualitativeWeight(), request.quantitativeWeight());
        validateYearRange(request.fromYear(), request.toYear());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskWeightByBusiness", item.getId(), AuditAction.UPDATE, "Cap nhat ty trong theo nghiep vu: " + item.getBusinessCode());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskWeightByBusiness item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskWeightByBusiness", id, AuditAction.DELETE, "Xoa ty trong theo nghiep vu: " + item.getBusinessCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_weight_by_business", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Tỷ trọng ĐT/ĐL theo Nghiệp vụ", exportColumns(), exportRows());
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
                String businessCode = row.get("businessCode");
                if (isBlank(businessCode)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma nghiep vu");
                }
                create(new WeightByBusinessRequest(businessCode.trim(), parseDecimal(row.get("qualitativeWeight")),
                        parseDecimal(row.get("quantitativeWeight")), parseInt(row.get("fromYear")), parseInt(row.get("toYear")), true));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskWeightByBusiness", null, AuditAction.CREATE,
                "Import Excel ty trong theo nghiep vu: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(RiskWeightByBusiness item, WeightByBusinessRequest request) {
        item.setBusinessCode(request.businessCode());
        item.setQualitativeWeight(request.qualitativeWeight());
        item.setQuantitativeWeight(request.quantitativeWeight());
        item.setFromYear(request.fromYear());
        item.setToYear(request.toYear());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String businessCode, Integer fromYear, UUID excludingId) {
        repository.findByTenantIdAndBusinessCodeAndFromYear(tenantId, businessCode, fromYear)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_WEIGHT_BIZ_DUPLICATE", "Da ton tai ty trong cho nghiep vu " + businessCode + " tu nam " + fromYear);
                });
    }

    /** Theo dung du lieu mau trong tai lieu goc (0.8+0.2, 0.9+0.1, 0.7+0.3...), Ty trong DT + Ty trong DL
     * luon phai bang 1 (100%) - chi check khi ca 2 gia tri deu duoc nhap. */
    private static final BigDecimal WEIGHT_SUM_TOLERANCE = new BigDecimal("0.001");

    private void validateWeightSum(BigDecimal qualitativeWeight, BigDecimal quantitativeWeight) {
        if (qualitativeWeight == null || quantitativeWeight == null) {
            return;
        }
        BigDecimal sum = qualitativeWeight.add(quantitativeWeight);
        if (sum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_SUM_TOLERANCE) > 0) {
            throw new BusinessException("RISK_WEIGHT_BIZ_SUM_INVALID", "Tong Ti trong DT + Ti trong DL phai bang 1 (100%)");
        }
    }

    private void validateYearRange(Integer fromYear, Integer toYear) {
        if (fromYear != null && toYear != null && fromYear > toYear) {
            throw new BusinessException("RISK_WEIGHT_BIZ_YEAR_RANGE_INVALID", "Tu nam phai nho hon hoac bang Den nam");
        }
    }

    private RiskWeightByBusiness getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_WEIGHT_BIZ_NOT_FOUND", "Khong tim thay ty trong theo nghiep vu", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("businessCode", "Ma nghiep vu"),
                new ExportColumn("qualitativeWeight", "Ti trong DT"),
                new ExportColumn("quantitativeWeight", "Ti trong DL"),
                new ExportColumn("fromYear", "Hieu luc tu"),
                new ExportColumn("toYear", "Hieu luc den"));
    }

    private List<Map<String, Object>> exportRows() {
        return repository.findByTenantIdOrderByBusinessCodeAscFromYearAsc(TenantContext.getTenantId()).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("businessCode", item.getBusinessCode());
                    row.put("qualitativeWeight", item.getQualitativeWeight());
                    row.put("quantitativeWeight", item.getQuantitativeWeight());
                    row.put("fromYear", item.getFromYear());
                    row.put("toYear", item.getToYear());
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

    private WeightByBusinessResponse toResponse(RiskWeightByBusiness item) {
        return new WeightByBusinessResponse(item.getId(), item.getBusinessCode(), item.getQualitativeWeight(),
                item.getQuantitativeWeight(), item.getFromYear(), item.getToYear(), item.isActive());
    }
}
