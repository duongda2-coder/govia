package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.AuditObjectProjectRequest;
import com.govia.audit.riskscoring.masterdata.dto.AuditObjectProjectResponse;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectProject;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectProjectRepository;
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

/** CRUD + Import/Export cho danh muc "Doi tuong kiem toan Du an/Dich vu thue ngoai" (sheet ZTC_DTKT3). */
@Service
public class AuditObjectProjectService {

    private final AuditObjectProjectRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditObjectProjectService(AuditObjectProjectRepository repository, AuditLogService auditLogService,
                                      ExcelExportService excelExportService, WordExportService wordExportService,
                                      ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditObjectProjectResponse> list() {
        return repository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditObjectProjectResponse create(AuditObjectProjectRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);

        AuditObjectProject item = new AuditObjectProject();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditObjectProject", item.getId(), AuditAction.CREATE, "Tao du an/DVTN: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public AuditObjectProjectResponse update(UUID id, AuditObjectProjectRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectProject item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditObjectProject", item.getId(), AuditAction.UPDATE, "Cap nhat du an/DVTN: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectProject item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditObjectProject", id, AuditAction.DELETE, "Xoa du an/DVTN: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_audit_object_project", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Đối tượng kiểm toán Dự án/Dịch vụ thuê ngoài", exportColumns(), exportRows());
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
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma du an/DV hoac Ten du an/DV");
                }
                create(new AuditObjectProjectRequest(code.trim(), name.trim(), emptyToNull(row.get("projectType")),
                        emptyToNull(row.get("approvalAuthority")), emptyToNull(row.get("purpose")),
                        parseDecimal(row.get("investmentValue")), emptyToNull(row.get("provider")),
                        emptyToNull(row.get("relatedParties")), parseInt(row.get("inspectionYear")),
                        emptyToNull(row.get("inspectionResult")), emptyToNull(row.get("inspectionRecommendation")),
                        parseInt(row.get("auditYear")), emptyToNull(row.get("auditResult")),
                        emptyToNull(row.get("auditRecommendation")), true));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditObjectProject", null, AuditAction.CREATE,
                "Import Excel du an/DVTN: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditObjectProject item, AuditObjectProjectRequest request) {
        item.setCode(request.code());
        item.setName(request.name());
        item.setProjectType(request.projectType());
        item.setApprovalAuthority(request.approvalAuthority());
        item.setPurpose(request.purpose());
        item.setInvestmentValue(request.investmentValue());
        item.setProvider(request.provider());
        item.setRelatedParties(request.relatedParties());
        item.setInspectionYear(request.inspectionYear());
        item.setInspectionResult(request.inspectionResult());
        item.setInspectionRecommendation(request.inspectionRecommendation());
        item.setAuditYear(request.auditYear());
        item.setAuditResult(request.auditResult());
        item.setAuditRecommendation(request.auditRecommendation());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_OBJECT_PROJECT_CODE_DUPLICATE", "Ma du an/DV da ton tai: " + code);
                });
    }

    private AuditObjectProject getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_PROJECT_NOT_FOUND", "Khong tim thay du an/DVTN", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("code", "Ma du an/DV"),
                new ExportColumn("name", "Ten du an/DV thue ngoai"),
                new ExportColumn("projectType", "Phan loai du an"),
                new ExportColumn("approvalAuthority", "Tham quyen QD"),
                new ExportColumn("purpose", "Muc dich dau tu/thue ngoai"),
                new ExportColumn("investmentValue", "Gia tri dau tu/thue ngoai"),
                new ExportColumn("provider", "Don vi cung cap"),
                new ExportColumn("relatedParties", "Cac ben lien quan"),
                new ExportColumn("inspectionYear", "Nam kiem tra"),
                new ExportColumn("inspectionResult", "Ket qua kiem tra"),
                new ExportColumn("inspectionRecommendation", "Kien nghi (kiem tra)"),
                new ExportColumn("auditYear", "Nam kiem toan"),
                new ExportColumn("auditResult", "Ket qua kiem toan"),
                new ExportColumn("auditRecommendation", "Kien nghi (kiem toan)"));
    }

    private List<Map<String, Object>> exportRows() {
        return repository.findByTenantIdOrderByCodeAsc(TenantContext.getTenantId()).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("projectType", item.getProjectType());
                    row.put("approvalAuthority", item.getApprovalAuthority());
                    row.put("purpose", item.getPurpose());
                    row.put("investmentValue", item.getInvestmentValue());
                    row.put("provider", item.getProvider());
                    row.put("relatedParties", item.getRelatedParties());
                    row.put("inspectionYear", item.getInspectionYear());
                    row.put("inspectionResult", item.getInspectionResult());
                    row.put("inspectionRecommendation", item.getInspectionRecommendation());
                    row.put("auditYear", item.getAuditYear());
                    row.put("auditResult", item.getAuditResult());
                    row.put("auditRecommendation", item.getAuditRecommendation());
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

    private AuditObjectProjectResponse toResponse(AuditObjectProject item) {
        return new AuditObjectProjectResponse(item.getId(), item.getCode(), item.getName(), item.getProjectType(),
                item.getApprovalAuthority(), item.getPurpose(), item.getInvestmentValue(), item.getProvider(),
                item.getRelatedParties(), item.getInspectionYear(), item.getInspectionResult(),
                item.getInspectionRecommendation(), item.getAuditYear(), item.getAuditResult(),
                item.getAuditRecommendation(), item.isActive());
    }
}
