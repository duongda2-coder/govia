package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.CriteriaQuantitativeRequest;
import com.govia.audit.riskscoring.masterdata.dto.CriteriaQuantitativeResponse;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQuantitative;
import com.govia.audit.riskscoring.masterdata.entity.RiskGroup1;
import com.govia.audit.riskscoring.masterdata.entity.RiskGroup2;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectCategoryRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskCriteriaQuantitativeRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskGroup1Repository;
import com.govia.audit.riskscoring.masterdata.repository.RiskGroup2Repository;
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

/** CRUD + Import/Export cho danh muc "Chi tieu danh gia rui ro dinh luong" (sheet ZTC_CTDGRR_DL). */
@Service
public class CriteriaQuantitativeService {

    private final RiskCriteriaQuantitativeRepository repository;
    private final RiskGroup1Repository group1Repository;
    private final RiskGroup2Repository group2Repository;
    private final AuditObjectCategoryRepository auditObjectCategoryRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public CriteriaQuantitativeService(RiskCriteriaQuantitativeRepository repository, RiskGroup1Repository group1Repository,
                                        RiskGroup2Repository group2Repository, AuditObjectCategoryRepository auditObjectCategoryRepository,
                                        AuditLogService auditLogService, ExcelExportService excelExportService,
                                        WordExportService wordExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.group1Repository = group1Repository;
        this.group2Repository = group2Repository;
        this.auditObjectCategoryRepository = auditObjectCategoryRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<CriteriaQuantitativeResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, String> group1Codes = group1CodesById(tenantId);
        Map<UUID, String> group2Codes = group2CodesById(tenantId);
        Map<UUID, AuditObjectCategory> categories = auditObjectCategoriesById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> toResponse(item, group1Codes, group2Codes, categories)).toList();
    }

    @Transactional
    public CriteriaQuantitativeResponse create(CriteriaQuantitativeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);
        validateGroups(tenantId, request.group1Id(), request.group2Id());
        validateCriteriaType(request.criteriaType());
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());

        RiskCriteriaQuantitative item = new RiskCriteriaQuantitative();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaQuantitative", item.getId(), AuditAction.CREATE, "Tao chi tieu dinh luong: " + item.getCode());
        return toResponse(item, group1CodesById(tenantId), group2CodesById(tenantId), auditObjectCategoriesById(tenantId));
    }

    @Transactional
    public CriteriaQuantitativeResponse update(UUID id, CriteriaQuantitativeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaQuantitative item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);
        validateGroups(tenantId, request.group1Id(), request.group2Id());
        validateCriteriaType(request.criteriaType());
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaQuantitative", item.getId(), AuditAction.UPDATE, "Cap nhat chi tieu dinh luong: " + item.getCode());
        return toResponse(item, group1CodesById(tenantId), group2CodesById(tenantId), auditObjectCategoriesById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaQuantitative item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskCriteriaQuantitative", id, AuditAction.DELETE, "Xoa chi tieu dinh luong: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_criteria_quantitative", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Chỉ tiêu đánh giá rủi ro định lượng", exportColumns(), exportRows());
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
        Map<String, UUID> categoryIdsByCode = new HashMap<>();
        auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> categoryIdsByCode.put(c.getCode(), c.getId()));
        Map<String, UUID> group1IdsByCode = new HashMap<>();
        group1Repository.findByTenantIdOrderByCodeAsc(tenantId).forEach(g -> group1IdsByCode.put(g.getCode(), g.getId()));
        Map<String, UUID> group2IdsByCode = new HashMap<>();
        group2Repository.findByTenantIdOrderByCodeAsc(tenantId).forEach(g -> group2IdsByCode.put(g.getCode(), g.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String auditObjectCategoryCode = row.get("auditObjectCategoryCode");
                String group1Code = row.get("group1Code");
                String code = row.get("code");
                String name = row.get("name");
                if (isBlank(auditObjectCategoryCode) || isBlank(group1Code) || isBlank(code) || isBlank(name)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Loai doi tuong kiem toan, Ma nhom cap 1, Ma hoac Ten");
                }
                UUID auditObjectCategoryId = categoryIdsByCode.get(auditObjectCategoryCode.trim());
                if (auditObjectCategoryId == null) {
                    throw new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan: " + auditObjectCategoryCode);
                }
                UUID group1Id = group1IdsByCode.get(group1Code.trim());
                if (group1Id == null) {
                    throw new BusinessException("RISK_GROUP1_NOT_FOUND", "Khong tim thay nhom cap 1: " + group1Code);
                }
                String group2Code = row.get("group2Code");
                UUID group2Id = isBlank(group2Code) ? null : group2IdsByCode.get(group2Code.trim());
                create(new CriteriaQuantitativeRequest(auditObjectCategoryId, group1Id, group2Id,
                        code.trim(), name.trim(), parseDecimal(row.get("weight")), parseInt(row.get("criteriaType")), parseDecimal(row.get("businessThreshold")),
                        parseDecimal(row.get("viewThreshold")), parseDecimal(row.get("score20")), parseDecimal(row.get("score40")),
                        parseDecimal(row.get("score60")), parseDecimal(row.get("score80")), parseDecimal(row.get("score100")),
                        row.get("scoringGuide"), true, true));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskCriteriaQuantitative", null, AuditAction.CREATE,
                "Import Excel chi tieu dinh luong: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(RiskCriteriaQuantitative item, CriteriaQuantitativeRequest request) {
        item.setAuditObjectCategoryId(request.auditObjectCategoryId());
        item.setGroup1Id(request.group1Id());
        item.setGroup2Id(request.group2Id());
        item.setCode(request.code());
        item.setName(request.name());
        item.setWeight(request.weight());
        item.setCriteriaType(request.criteriaType());
        item.setBusinessThreshold(request.businessThreshold());
        item.setViewThreshold(request.viewThreshold());
        item.setScore20(request.score20());
        item.setScore40(request.score40());
        item.setScore60(request.score60());
        item.setScore80(request.score80());
        item.setScore100(request.score100());
        item.setScoringGuide(request.scoringGuide());
        item.setIncludeCurrentYear(request.includeCurrentYear());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_CRITERIA_DL_CODE_DUPLICATE", "Ma chi tieu da ton tai: " + code);
                });
    }

    private void validateGroups(UUID tenantId, UUID group1Id, UUID group2Id) {
        group1Repository.findById(group1Id)
                .filter(g -> g.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_GROUP1_NOT_FOUND", "Khong tim thay nhom chi tieu cap 1"));
        if (group2Id != null) {
            RiskGroup2 group2 = group2Repository.findById(group2Id)
                    .filter(g -> g.getTenantId().equals(tenantId))
                    .orElseThrow(() -> new BusinessException("RISK_GROUP2_NOT_FOUND", "Khong tim thay nhom chi tieu cap 2"));
            if (!group2.getGroup1Id().equals(group1Id)) {
                throw new BusinessException("RISK_GROUP2_NOT_IN_GROUP1", "Nhom chi tieu cap 2 khong thuoc nhom cap 1 da chon");
            }
        }
    }

    private void validateAuditObjectCategory(UUID tenantId, UUID auditObjectCategoryId) {
        auditObjectCategoryRepository.findById(auditObjectCategoryId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan"));
    }

    /** Theo dung mo ta trong tai lieu goc ("Loai CT": Number, List 1,2,3) - chi 3 gia tri nay hop le. */
    private void validateCriteriaType(Integer criteriaType) {
        if (criteriaType != null && criteriaType != 1 && criteriaType != 2 && criteriaType != 3) {
            throw new BusinessException("RISK_CRITERIA_DL_TYPE_INVALID", "Loai chi tieu chi nhan gia tri 1, 2 hoac 3");
        }
    }

    private RiskCriteriaQuantitative getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_CRITERIA_DL_NOT_FOUND", "Khong tim thay chi tieu dinh luong", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, String> group1CodesById(UUID tenantId) {
        Map<UUID, String> map = new HashMap<>();
        for (RiskGroup1 g : group1Repository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(g.getId(), g.getCode());
        }
        return map;
    }

    private Map<UUID, String> group2CodesById(UUID tenantId) {
        Map<UUID, String> map = new HashMap<>();
        for (RiskGroup2 g : group2Repository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(g.getId(), g.getCode());
        }
        return map;
    }

    private Map<UUID, AuditObjectCategory> auditObjectCategoriesById(UUID tenantId) {
        Map<UUID, AuditObjectCategory> map = new HashMap<>();
        for (AuditObjectCategory c : auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("auditObjectCategoryCode", "Loai doi tuong kiem toan"),
                new ExportColumn("group1Code", "Nhom cap 1"),
                new ExportColumn("group2Code", "Nhom cap 2"),
                new ExportColumn("code", "Ma chi tieu"),
                new ExportColumn("name", "Ten chi tieu"),
                new ExportColumn("weight", "Ti trong"),
                new ExportColumn("criteriaType", "Loai CT"),
                new ExportColumn("businessThreshold", "Nguong NV"),
                new ExportColumn("viewThreshold", "Nguong hien thi"),
                new ExportColumn("score20", "Diem 20"),
                new ExportColumn("score40", "Diem 40"),
                new ExportColumn("score60", "Diem 60"),
                new ExportColumn("score80", "Diem 80"),
                new ExportColumn("score100", "Diem 100"),
                new ExportColumn("scoringGuide", "Huong dan cham diem"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, String> group1Codes = group1CodesById(tenantId);
        Map<UUID, String> group2Codes = group2CodesById(tenantId);
        Map<UUID, AuditObjectCategory> categories = auditObjectCategoriesById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> {
                    AuditObjectCategory category = categories.get(item.getAuditObjectCategoryId());
                    Map<String, Object> row = new HashMap<>();
                    row.put("auditObjectCategoryCode", category != null ? category.getCode() : null);
                    row.put("group1Code", group1Codes.get(item.getGroup1Id()));
                    row.put("group2Code", group2Codes.get(item.getGroup2Id()));
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("weight", item.getWeight());
                    row.put("criteriaType", item.getCriteriaType());
                    row.put("businessThreshold", item.getBusinessThreshold());
                    row.put("viewThreshold", item.getViewThreshold());
                    row.put("score20", item.getScore20());
                    row.put("score40", item.getScore40());
                    row.put("score60", item.getScore60());
                    row.put("score80", item.getScore80());
                    row.put("score100", item.getScore100());
                    row.put("scoringGuide", item.getScoringGuide());
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

    private CriteriaQuantitativeResponse toResponse(RiskCriteriaQuantitative item, Map<UUID, String> group1Codes, Map<UUID, String> group2Codes,
                                                      Map<UUID, AuditObjectCategory> categories) {
        AuditObjectCategory category = categories.get(item.getAuditObjectCategoryId());
        return new CriteriaQuantitativeResponse(item.getId(), item.getAuditObjectCategoryId(),
                category != null ? category.getCode() : null, category != null ? category.getName() : null,
                item.getGroup1Id(), group1Codes.get(item.getGroup1Id()), item.getGroup2Id(), group2Codes.get(item.getGroup2Id()),
                item.getCode(), item.getName(), item.getWeight(), item.getCriteriaType(), item.getBusinessThreshold(), item.getViewThreshold(),
                item.getScore20(), item.getScore40(), item.getScore60(), item.getScore80(), item.getScore100(),
                item.getScoringGuide(), item.isIncludeCurrentYear(), item.isActive());
    }
}
