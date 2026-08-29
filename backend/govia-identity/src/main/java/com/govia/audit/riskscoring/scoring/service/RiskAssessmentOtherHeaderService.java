package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectCategoryRepository;
import com.govia.audit.riskscoring.masterdata.service.AuditObjectResolverService;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherHeaderRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskAssessmentOtherHeaderResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskAssessmentOtherHeader;
import com.govia.audit.riskscoring.scoring.repository.RiskAssessmentOtherHeaderRepository;
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
 * CRUD cho header cua man hinh "Cham diem rui ro HO, CNTT, Du an, Dich vu thue ngoai..." (sheet
 * ZTC_CDRR_KHAC). "Ma doi tuong KT" tro toi 1 trong 4 danh muc doi tuong kiem toan cu the, tuy theo
 * objectSource cua Loai doi tuong KT (category) - xem AuditObjectResolverService.
 */
@Service
public class RiskAssessmentOtherHeaderService {

    private final RiskAssessmentOtherHeaderRepository repository;
    private final AuditObjectCategoryRepository auditObjectCategoryRepository;
    private final AuditObjectResolverService objectResolver;
    private final RiskAssessmentOtherLineService lineService;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public RiskAssessmentOtherHeaderService(RiskAssessmentOtherHeaderRepository repository,
                                             AuditObjectCategoryRepository auditObjectCategoryRepository,
                                             AuditObjectResolverService objectResolver,
                                             RiskAssessmentOtherLineService lineService,
                                             AuditLogService auditLogService,
                                             ExcelExportService excelExportService,
                                             WordExportService wordExportService,
                                             ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditObjectCategoryRepository = auditObjectCategoryRepository;
        this.objectResolver = objectResolver;
        this.lineService = lineService;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<RiskAssessmentOtherHeaderResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditObjectCategory> categories = categoriesById(tenantId);
        return repository.findByTenantIdOrderByYearDescAuditObjectCodeAsc(tenantId).stream()
                .map(item -> toResponse(item, categories))
                .toList();
    }

    @Transactional
    public RiskAssessmentOtherHeaderResponse create(RiskAssessmentOtherHeaderRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectCategory category = validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());
        validateAuditObjectCode(tenantId, category, request.auditObjectCode());
        checkNoDuplicate(tenantId, request.auditObjectCategoryId(), request.auditObjectCode(), request.year(), null);

        RiskAssessmentOtherHeader item = new RiskAssessmentOtherHeader();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        lineService.ensureLines(item);

        auditLogService.record("RiskAssessmentOtherHeader", item.getId(), AuditAction.CREATE,
                "Tao ky cham diem rui ro khac: " + item.getAuditObjectCode() + "/" + item.getYear());
        return toResponse(item, categoriesById(tenantId));
    }

    @Transactional
    public RiskAssessmentOtherHeaderResponse update(UUID id, RiskAssessmentOtherHeaderRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskAssessmentOtherHeader item = getOwnedOrThrow(tenantId, id);
        AuditObjectCategory category = validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());
        validateAuditObjectCode(tenantId, category, request.auditObjectCode());
        checkNoDuplicate(tenantId, request.auditObjectCategoryId(), request.auditObjectCode(), request.year(), id);

        applyRequest(item, request);
        item = repository.save(item);

        lineService.ensureLines(item);

        auditLogService.record("RiskAssessmentOtherHeader", item.getId(), AuditAction.UPDATE,
                "Cap nhat ky cham diem rui ro khac: " + item.getAuditObjectCode() + "/" + item.getYear());
        return toResponse(item, categoriesById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskAssessmentOtherHeader item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskAssessmentOtherHeader", id, AuditAction.DELETE,
                "Xoa ky cham diem rui ro khac: " + item.getAuditObjectCode() + "/" + item.getYear());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_assessment_other", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Chấm điểm rủi ro HO, CNTT, Dự án, Dịch vụ thuê ngoài", exportColumns(), exportRows());
    }

    /**
     * Import Excel theo mau da xuat: moi dong ung voi 1 "Line" (chi tieu + diem) cua 1 header (Loai
     * doi tuong KT + Ma doi tuong KT + Nam) - header duoc tao tu dong (kem sinh du cac dong chi
     * tieu phu hop) neu chua ton tai, cac dong sau cung header chi cap nhat diem tung chi tieu.
     */
    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), exportColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        UUID tenantId = TenantContext.getTenantId();
        Map<String, AuditObjectCategory> categoriesByCode = new HashMap<>();
        auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> categoriesByCode.put(c.getCode(), c));
        Map<String, RiskAssessmentOtherHeader> headerCache = new HashMap<>();

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String categoryCode = row.get("auditObjectCategoryCode");
                String objectCode = row.get("auditObjectCode");
                String yearStr = row.get("year");
                String criteriaCode = row.get("criteriaOtherCode");
                String scoreStr = row.get("score");
                if (isBlank(categoryCode) || isBlank(objectCode) || isBlank(yearStr) || isBlank(criteriaCode) || isBlank(scoreStr)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED",
                            "Thieu Loai doi tuong KT, Ma doi tuong KT, Nam, Ma chi tieu hoac Diem");
                }
                AuditObjectCategory category = categoriesByCode.get(categoryCode.trim());
                if (category == null) {
                    throw new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan: " + categoryCode);
                }
                Integer year = parseInt(yearStr);
                if (year == null) {
                    throw new BusinessException("IMPORT_INVALID_YEAR", "Nam khong hop le: " + yearStr);
                }
                validateAuditObjectCode(tenantId, category, objectCode.trim());

                String headerKey = category.getId() + "/" + objectCode.trim() + "/" + year;
                RiskAssessmentOtherHeader header = headerCache.computeIfAbsent(headerKey, k ->
                        repository.findByTenantIdAndAuditObjectCategoryIdAndAuditObjectCodeAndYear(tenantId, category.getId(), objectCode.trim(), year)
                                .orElseGet(() -> {
                                    RiskAssessmentOtherHeader h = new RiskAssessmentOtherHeader();
                                    h.setTenantId(tenantId);
                                    h.setAuditObjectCategoryId(category.getId());
                                    h.setAuditObjectCode(objectCode.trim());
                                    h.setYear(year);
                                    h.setActive(true);
                                    h = repository.save(h);
                                    lineService.ensureLines(h);
                                    return h;
                                }));

                Integer score = parseInt(scoreStr);
                if (score == null) {
                    throw new BusinessException("IMPORT_INVALID_SCORE", "Diem khong hop le: " + scoreStr);
                }
                lineService.setScoreByCriteriaCode(tenantId, header, criteriaCode.trim(), score);
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskAssessmentOtherHeader", null, AuditAction.CREATE,
                "Import Excel cham diem rui ro khac: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("auditObjectCategoryCode", "Loai doi tuong KT"),
                new ExportColumn("auditObjectCode", "Ma doi tuong KT"),
                new ExportColumn("year", "Nam"),
                new ExportColumn("criteriaOtherCode", "Ma chi tieu"),
                new ExportColumn("criteriaOtherName", "Ten chi tieu"),
                new ExportColumn("score", "Diem"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RiskAssessmentOtherHeader header : repository.findByTenantIdOrderByYearDescAuditObjectCodeAsc(tenantId)) {
            AuditObjectCategory category = auditObjectCategoryRepository.findById(header.getAuditObjectCategoryId()).orElse(null);
            lineService.listByHeaderReadOnly(header).forEach(line -> {
                Map<String, Object> row = new HashMap<>();
                row.put("auditObjectCategoryCode", category != null ? category.getCode() : null);
                row.put("auditObjectCode", header.getAuditObjectCode());
                row.put("year", header.getYear());
                row.put("criteriaOtherCode", line.criteriaOtherCode());
                row.put("criteriaOtherName", line.criteriaOtherName());
                row.put("score", line.scaleScore());
                rows.add(row);
            });
        }
        return rows;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    @Transactional(readOnly = true)
    public RiskAssessmentOtherHeader getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_ASSESSMENT_OTHER_HEADER_NOT_FOUND", "Khong tim thay ky cham diem", HttpStatus.NOT_FOUND));
    }

    private void applyRequest(RiskAssessmentOtherHeader item, RiskAssessmentOtherHeaderRequest request) {
        item.setAuditObjectCategoryId(request.auditObjectCategoryId());
        item.setAuditObjectCode(request.auditObjectCode());
        item.setYear(request.year());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, UUID auditObjectCategoryId, String auditObjectCode, Integer year, UUID excludingId) {
        repository.findByTenantIdAndAuditObjectCategoryIdAndAuditObjectCodeAndYear(tenantId, auditObjectCategoryId, auditObjectCode, year)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_ASSESSMENT_OTHER_HEADER_DUPLICATE",
                            "Da ton tai ky cham diem cho doi tuong " + auditObjectCode + " nam " + year);
                });
    }

    private AuditObjectCategory validateAuditObjectCategory(UUID tenantId, UUID auditObjectCategoryId) {
        return auditObjectCategoryRepository.findById(auditObjectCategoryId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan"));
    }

    private void validateAuditObjectCode(UUID tenantId, AuditObjectCategory category, String auditObjectCode) {
        if (!objectResolver.exists(tenantId, category, auditObjectCode)) {
            throw new BusinessException("AUDIT_OBJECT_CODE_NOT_FOUND", "Khong tim thay ma doi tuong kiem toan: " + auditObjectCode);
        }
    }

    private Map<UUID, AuditObjectCategory> categoriesById(UUID tenantId) {
        Map<UUID, AuditObjectCategory> map = new HashMap<>();
        for (AuditObjectCategory c : auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private RiskAssessmentOtherHeaderResponse toResponse(RiskAssessmentOtherHeader item, Map<UUID, AuditObjectCategory> categories) {
        AuditObjectCategory category = categories.get(item.getAuditObjectCategoryId());
        String auditObjectName = category != null ? objectResolver.resolveName(item.getTenantId(), category, item.getAuditObjectCode()) : null;
        return new RiskAssessmentOtherHeaderResponse(item.getId(),
                item.getAuditObjectCategoryId(), category != null ? category.getCode() : null, category != null ? category.getName() : null,
                item.getAuditObjectCode(), auditObjectName, item.getYear(), item.isActive());
    }
}
