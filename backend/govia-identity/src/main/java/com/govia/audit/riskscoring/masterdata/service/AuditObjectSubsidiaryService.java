package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.AuditObjectSubsidiaryRequest;
import com.govia.audit.riskscoring.masterdata.dto.AuditObjectSubsidiaryResponse;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectSubsidiary;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectSubsidiaryRepository;
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

/** CRUD + Import/Export cho danh muc "Doi tuong kiem toan Cong ty con" (sheet ZTC_DTKT2). */
@Service
public class AuditObjectSubsidiaryService {

    private final AuditObjectSubsidiaryRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditObjectSubsidiaryService(AuditObjectSubsidiaryRepository repository, AuditLogService auditLogService,
                                         ExcelExportService excelExportService, WordExportService wordExportService,
                                         ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditObjectSubsidiaryResponse> list() {
        return repository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditObjectSubsidiaryResponse create(AuditObjectSubsidiaryRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);

        AuditObjectSubsidiary item = new AuditObjectSubsidiary();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditObjectSubsidiary", item.getId(), AuditAction.CREATE, "Tao cong ty con: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public AuditObjectSubsidiaryResponse update(UUID id, AuditObjectSubsidiaryRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectSubsidiary item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditObjectSubsidiary", item.getId(), AuditAction.UPDATE, "Cap nhat cong ty con: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectSubsidiary item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditObjectSubsidiary", id, AuditAction.DELETE, "Xoa cong ty con: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_audit_object_subsidiary", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Đối tượng kiểm toán Công ty con", exportColumns(), exportRows());
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
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma cong ty hoac Ten cong ty");
                }
                create(new AuditObjectSubsidiaryRequest(code.trim(), name.trim(), emptyToNull(row.get("companyType")),
                        parseDate(row.get("establishedDate")), parseInt(row.get("staffCount")), parseInt(row.get("leaderCount")),
                        parseInt(row.get("inspectionYear")), emptyToNull(row.get("inspectionResult")),
                        emptyToNull(row.get("inspectionRecommendation")), parseInt(row.get("auditYear")),
                        emptyToNull(row.get("auditResult")), emptyToNull(row.get("auditRecommendation")),
                        parseDecimal(row.get("revenue")), parseDecimal(row.get("cost")), parseDecimal(row.get("profit")),
                        parseDecimal(row.get("salaryFund")), true));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditObjectSubsidiary", null, AuditAction.CREATE,
                "Import Excel cong ty con: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditObjectSubsidiary item, AuditObjectSubsidiaryRequest request) {
        item.setCode(request.code());
        item.setName(request.name());
        item.setCompanyType(request.companyType());
        item.setEstablishedDate(request.establishedDate());
        item.setStaffCount(request.staffCount());
        item.setLeaderCount(request.leaderCount());
        item.setInspectionYear(request.inspectionYear());
        item.setInspectionResult(request.inspectionResult());
        item.setInspectionRecommendation(request.inspectionRecommendation());
        item.setAuditYear(request.auditYear());
        item.setAuditResult(request.auditResult());
        item.setAuditRecommendation(request.auditRecommendation());
        item.setRevenue(request.revenue());
        item.setCost(request.cost());
        item.setProfit(request.profit());
        item.setSalaryFund(request.salaryFund());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_OBJECT_SUBSIDIARY_CODE_DUPLICATE", "Ma cong ty da ton tai: " + code);
                });
    }

    private AuditObjectSubsidiary getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_SUBSIDIARY_NOT_FOUND", "Khong tim thay cong ty con", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("code", "Ma cong ty"),
                new ExportColumn("name", "Ten cong ty"),
                new ExportColumn("companyType", "Loai cong ty"),
                new ExportColumn("establishedDate", "Ngay thanh lap"),
                new ExportColumn("staffCount", "So luong CB"),
                new ExportColumn("leaderCount", "So luong LD"),
                new ExportColumn("inspectionYear", "Nam kiem tra"),
                new ExportColumn("inspectionResult", "Ket qua kiem tra"),
                new ExportColumn("inspectionRecommendation", "Kien nghi (kiem tra)"),
                new ExportColumn("auditYear", "Nam kiem toan"),
                new ExportColumn("auditResult", "Ket qua kiem toan"),
                new ExportColumn("auditRecommendation", "Kien nghi (kiem toan)"),
                new ExportColumn("revenue", "Doanh thu"),
                new ExportColumn("cost", "Chi phi"),
                new ExportColumn("profit", "Loi nhuan"),
                new ExportColumn("salaryFund", "Quy tien luong"));
    }

    private List<Map<String, Object>> exportRows() {
        return repository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("companyType", item.getCompanyType());
                    row.put("establishedDate", item.getEstablishedDate());
                    row.put("staffCount", item.getStaffCount());
                    row.put("leaderCount", item.getLeaderCount());
                    row.put("inspectionYear", item.getInspectionYear());
                    row.put("inspectionResult", item.getInspectionResult());
                    row.put("inspectionRecommendation", item.getInspectionRecommendation());
                    row.put("auditYear", item.getAuditYear());
                    row.put("auditResult", item.getAuditResult());
                    row.put("auditRecommendation", item.getAuditRecommendation());
                    row.put("revenue", item.getRevenue());
                    row.put("cost", item.getCost());
                    row.put("profit", item.getProfit());
                    row.put("salaryFund", item.getSalaryFund());
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

    private AuditObjectSubsidiaryResponse toResponse(AuditObjectSubsidiary item) {
        return new AuditObjectSubsidiaryResponse(item.getId(), item.getCode(), item.getName(), item.getCompanyType(),
                item.getEstablishedDate(), item.getStaffCount(), item.getLeaderCount(), item.getInspectionYear(),
                item.getInspectionResult(), item.getInspectionRecommendation(), item.getAuditYear(), item.getAuditResult(),
                item.getAuditRecommendation(), item.getRevenue(), item.getCost(), item.getProfit(), item.getSalaryFund(),
                item.isActive());
    }
}
