package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQualitative;
import com.govia.audit.riskscoring.masterdata.entity.RiskGroup1;
import com.govia.audit.riskscoring.masterdata.entity.RiskGroup2;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskCriteriaQualitativeRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskGroup1Repository;
import com.govia.audit.riskscoring.masterdata.repository.RiskGroup2Repository;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQualitativeValueRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaQualitativeValueResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaQualitativeValue;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaQualitativeValueRepository;
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

/**
 * "Ho so rui ro dinh tinh" (sheet ZTC_HSRR, upload theo mau DT_HSRR_Upload) - dang bang dai
 * (long-format, 1 dong = 1 chi tieu/chi nhanh/nam) nen dung duoc ExcelImportService dung chung.
 * Khac voi ho so dinh luong, khong co co che phan quyen theo user/chi tieu (ZTC_HSRR_DL_User chi
 * ap dung cho "DL" = dinh luong theo dung ten goi va mo ta trong tai lieu goc).
 */
@Service
public class RiskCriteriaQualitativeValueService {

    private final RiskCriteriaQualitativeValueRepository repository;
    private final RiskCriteriaQualitativeRepository criteriaRepository;
    private final RiskGroup1Repository group1Repository;
    private final RiskGroup2Repository group2Repository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final AuditLogService auditLogService;
    private final ExcelImportService excelImportService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;

    public RiskCriteriaQualitativeValueService(RiskCriteriaQualitativeValueRepository repository,
                                                RiskCriteriaQualitativeRepository criteriaRepository,
                                                RiskGroup1Repository group1Repository,
                                                RiskGroup2Repository group2Repository,
                                                AuditObjectUnitRepository auditObjectUnitRepository,
                                                AuditLogService auditLogService,
                                                ExcelImportService excelImportService,
                                                ExcelExportService excelExportService,
                                                WordExportService wordExportService) {
        this.repository = repository;
        this.criteriaRepository = criteriaRepository;
        this.group1Repository = group1Repository;
        this.group2Repository = group2Repository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.auditLogService = auditLogService;
        this.excelImportService = excelImportService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
    }

    @Transactional(readOnly = true)
    public List<RiskCriteriaQualitativeValueResponse> list(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, RiskCriteriaQualitative> criteria = criteriaById(tenantId);
        Map<String, AuditObjectUnit> units = unitsByCode(tenantId);
        Map<UUID, RiskGroup1> groups1 = group1ById(tenantId);
        Map<UUID, RiskGroup2> groups2 = group2ById(tenantId);
        return repository.findByTenantIdAndYearOrderByBranchCodeAsc(tenantId, year).stream()
                .map(item -> toResponse(item, criteria, units, groups1, groups2))
                .toList();
    }

    @Transactional
    public RiskCriteriaQualitativeValueResponse create(RiskCriteriaQualitativeValueRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        validateCriteria(tenantId, request.criteriaId());
        validateBranch(tenantId, request.branchCode());
        checkNoDuplicate(tenantId, request.criteriaId(), request.branchCode(), request.year(), null);

        RiskCriteriaQualitativeValue item = new RiskCriteriaQualitativeValue();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaQualitativeValue", item.getId(), AuditAction.CREATE,
                "Tao HSRR dinh tinh: " + item.getBranchCode() + "/" + item.getYear());
        return toResponse(item, criteriaById(tenantId), unitsByCode(tenantId), group1ById(tenantId), group2ById(tenantId));
    }

    @Transactional
    public RiskCriteriaQualitativeValueResponse update(UUID id, RiskCriteriaQualitativeValueRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaQualitativeValue item = getOwnedOrThrow(tenantId, id);
        validateCriteria(tenantId, request.criteriaId());
        validateBranch(tenantId, request.branchCode());
        checkNoDuplicate(tenantId, request.criteriaId(), request.branchCode(), request.year(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaQualitativeValue", item.getId(), AuditAction.UPDATE,
                "Cap nhat HSRR dinh tinh: " + item.getBranchCode() + "/" + item.getYear());
        return toResponse(item, criteriaById(tenantId), unitsByCode(tenantId), group1ById(tenantId), group2ById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaQualitativeValue item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskCriteriaQualitativeValue", id, AuditAction.DELETE,
                "Xoa HSRR dinh tinh: " + item.getBranchCode() + "/" + item.getYear());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(Integer year) {
        return excelExportService.export("risk_score_criteria_qualitative_value", templateColumns(), exportRows(year));
    }

    @Transactional(readOnly = true)
    public byte[] exportWord(Integer year) {
        return wordExportService.export("Hồ sơ rủi ro định tính", templateColumns(), exportRows(year));
    }

    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), templateColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        UUID tenantId = TenantContext.getTenantId();
        Map<String, UUID> criteriaIdsByCode = new HashMap<>();
        criteriaRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> criteriaIdsByCode.put(c.getCode(), c.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String criteriaCode = row.get("criteriaCode");
                String branchCode = row.get("branchCode");
                Integer year = parseInt(row.get("year"));
                if (isBlank(criteriaCode) || isBlank(branchCode) || year == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi tieu, Ma chi nhanh hoac Nam");
                }
                UUID criteriaId = criteriaIdsByCode.get(criteriaCode.trim());
                if (criteriaId == null) {
                    throw new BusinessException("RISK_CRITERIA_DT_NOT_FOUND", "Khong tim thay chi tieu dinh tinh: " + criteriaCode);
                }
                if (auditObjectUnitRepository.findByTenantIdAndCode(tenantId, branchCode.trim()).isEmpty()) {
                    throw new BusinessException("AUDIT_OBJECT_CODE_NOT_FOUND", "Khong tim thay chi nhanh: " + branchCode);
                }

                RiskCriteriaQualitativeValue item = repository
                        .findByTenantIdAndCriteriaIdAndBranchCodeAndYear(tenantId, criteriaId, branchCode.trim(), year)
                        .orElseGet(() -> {
                            RiskCriteriaQualitativeValue created = new RiskCriteriaQualitativeValue();
                            created.setTenantId(tenantId);
                            created.setCriteriaId(criteriaId);
                            created.setBranchCode(branchCode.trim());
                            created.setYear(year);
                            return created;
                        });
                item.setViolation(emptyToNull(row.get("violation")));
                item.setNote(emptyToNull(row.get("note")));
                repository.save(item);
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskCriteriaQualitativeValue", null, AuditAction.CREATE,
                "Upload HSRR dinh tinh: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    /** Khop dung tieu de + thu tu cot cua mau DT_HSRR_Upload. "Ma nhom"/"Ma nhom cap 2"/"Ten chi
     * tieu" chi de doc (suy ra tu chi tieu qua RiskCriteriaQualitative.group1Id/group2Id), import
     * chi dung criteriaCode/branchCode/year/violation/note - cac cot con lai bi bo qua neu co mat. */
    private List<ExportColumn> templateColumns() {
        return List.of(
                new ExportColumn("group1Code", "Mã nhóm"),
                new ExportColumn("group2Code", "Mã nhóm cấp 2"),
                new ExportColumn("criteriaCode", "Mã chỉ tiêu"),
                new ExportColumn("criteriaName", "Tên chỉ tiêu"),
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("year", "Năm"),
                new ExportColumn("violation", "Sai phạm"),
                new ExportColumn("note", "Ghi chú"));
    }

    private List<Map<String, Object>> exportRows(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, RiskCriteriaQualitative> criteria = criteriaById(tenantId);
        Map<UUID, RiskGroup1> groups1 = group1ById(tenantId);
        Map<UUID, RiskGroup2> groups2 = group2ById(tenantId);
        return repository.findByTenantIdAndYearOrderByBranchCodeAsc(tenantId, year).stream()
                .map(item -> {
                    RiskCriteriaQualitative criterion = criteria.get(item.getCriteriaId());
                    RiskGroup1 group1 = criterion != null ? groups1.get(criterion.getGroup1Id()) : null;
                    RiskGroup2 group2 = criterion != null ? groups2.get(criterion.getGroup2Id()) : null;
                    Map<String, Object> row = new HashMap<>();
                    row.put("group1Code", group1 != null ? group1.getCode() : null);
                    row.put("group2Code", group2 != null ? group2.getCode() : null);
                    row.put("criteriaCode", criterion != null ? criterion.getCode() : null);
                    row.put("criteriaName", criterion != null ? criterion.getName() : null);
                    row.put("branchCode", item.getBranchCode());
                    row.put("year", item.getYear());
                    row.put("violation", item.getViolation());
                    row.put("note", item.getNote());
                    return row;
                }).toList();
    }

    private void applyRequest(RiskCriteriaQualitativeValue item, RiskCriteriaQualitativeValueRequest request) {
        item.setCriteriaId(request.criteriaId());
        item.setBranchCode(request.branchCode());
        item.setYear(request.year());
        item.setViolation(emptyToNull(request.violation()));
        item.setNote(emptyToNull(request.note()));
    }

    private void checkNoDuplicate(UUID tenantId, UUID criteriaId, String branchCode, Integer year, UUID excludingId) {
        repository.findByTenantIdAndCriteriaIdAndBranchCodeAndYear(tenantId, criteriaId, branchCode, year)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_CRITERIA_QUALITATIVE_VALUE_DUPLICATE",
                            "Da ton tai gia tri cho chi nhanh " + branchCode + " nam " + year + " voi chi tieu nay");
                });
    }

    private void validateCriteria(UUID tenantId, UUID criteriaId) {
        criteriaRepository.findById(criteriaId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_CRITERIA_DT_NOT_FOUND", "Khong tim thay chi tieu dinh tinh"));
    }

    private void validateBranch(UUID tenantId, String branchCode) {
        if (auditObjectUnitRepository.findByTenantIdAndCode(tenantId, branchCode).isEmpty()) {
            throw new BusinessException("AUDIT_OBJECT_CODE_NOT_FOUND", "Khong tim thay chi nhanh: " + branchCode);
        }
    }

    private RiskCriteriaQualitativeValue getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_CRITERIA_QUALITATIVE_VALUE_NOT_FOUND", "Khong tim thay gia tri HSRR", HttpStatus.NOT_FOUND));
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

    private Map<UUID, RiskCriteriaQualitative> criteriaById(UUID tenantId) {
        Map<UUID, RiskCriteriaQualitative> map = new HashMap<>();
        criteriaRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> map.put(c.getId(), c));
        return map;
    }

    private Map<UUID, RiskGroup1> group1ById(UUID tenantId) {
        Map<UUID, RiskGroup1> map = new HashMap<>();
        group1Repository.findByTenantIdOrderByCodeAsc(tenantId).forEach(g -> map.put(g.getId(), g));
        return map;
    }

    private Map<UUID, RiskGroup2> group2ById(UUID tenantId) {
        Map<UUID, RiskGroup2> map = new HashMap<>();
        group2Repository.findByTenantIdOrderByCodeAsc(tenantId).forEach(g -> map.put(g.getId(), g));
        return map;
    }

    private Map<String, AuditObjectUnit> unitsByCode(UUID tenantId) {
        Map<String, AuditObjectUnit> map = new HashMap<>();
        auditObjectUnitRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(u -> map.put(u.getCode(), u));
        return map;
    }

    private RiskCriteriaQualitativeValueResponse toResponse(RiskCriteriaQualitativeValue item,
                                                              Map<UUID, RiskCriteriaQualitative> criteria,
                                                              Map<String, AuditObjectUnit> units,
                                                              Map<UUID, RiskGroup1> groups1,
                                                              Map<UUID, RiskGroup2> groups2) {
        RiskCriteriaQualitative criterion = criteria.get(item.getCriteriaId());
        AuditObjectUnit unit = units.get(item.getBranchCode());
        RiskGroup1 group1 = criterion != null ? groups1.get(criterion.getGroup1Id()) : null;
        RiskGroup2 group2 = criterion != null ? groups2.get(criterion.getGroup2Id()) : null;
        return new RiskCriteriaQualitativeValueResponse(item.getId(), item.getYear(), item.getBranchCode(),
                unit != null ? unit.getName() : null,
                item.getCriteriaId(), criterion != null ? criterion.getCode() : null, criterion != null ? criterion.getName() : null,
                group1 != null ? group1.getCode() : null, group2 != null ? group2.getCode() : null,
                item.getViolation(), item.getNote());
    }
}
