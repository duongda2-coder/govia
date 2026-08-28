package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectCategoryRepository;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaOtherScaleRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaOtherScaleResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaOther;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaOtherScale;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaOtherRepository;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaOtherScaleRepository;
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
 * CRUD + Import/Export cho danh muc "Thang diem cua chi tieu danh gia rui ro HO, CNTT, Du an,
 * Dich vu thue ngoai..." (sheet ZTC_CTRR_KHAC_TD). Moi dong la 1 muc thang diem cua 1 chi tieu
 * (xem RiskCriteriaOther) - 1 chi tieu co the co nhieu dong thang diem.
 */
@Service
public class RiskCriteriaOtherScaleService {

    private final RiskCriteriaOtherScaleRepository repository;
    private final AuditObjectCategoryRepository auditObjectCategoryRepository;
    private final RiskCriteriaOtherRepository criteriaOtherRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public RiskCriteriaOtherScaleService(RiskCriteriaOtherScaleRepository repository,
                                          AuditObjectCategoryRepository auditObjectCategoryRepository,
                                          RiskCriteriaOtherRepository criteriaOtherRepository,
                                          AuditLogService auditLogService, ExcelExportService excelExportService,
                                          WordExportService wordExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditObjectCategoryRepository = auditObjectCategoryRepository;
        this.criteriaOtherRepository = criteriaOtherRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<RiskCriteriaOtherScaleResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCriteriaOtherIdAscScaleScoreAsc(tenantId).stream()
                .map(item -> toResponse(item, categoriesById(tenantId), criteriaById(tenantId)))
                .toList();
    }

    @Transactional
    public RiskCriteriaOtherScaleResponse create(RiskCriteriaOtherScaleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());
        validateCriteriaOther(tenantId, request.criteriaOtherId());

        RiskCriteriaOtherScale item = new RiskCriteriaOtherScale();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaOtherScale", item.getId(), AuditAction.CREATE, "Tao thang diem chi tieu DGRR khac");
        return toResponse(item, categoriesById(tenantId), criteriaById(tenantId));
    }

    @Transactional
    public RiskCriteriaOtherScaleResponse update(UUID id, RiskCriteriaOtherScaleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaOtherScale item = getOwnedOrThrow(tenantId, id);
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());
        validateCriteriaOther(tenantId, request.criteriaOtherId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaOtherScale", item.getId(), AuditAction.UPDATE, "Cap nhat thang diem chi tieu DGRR khac");
        return toResponse(item, categoriesById(tenantId), criteriaById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaOtherScale item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskCriteriaOtherScale", id, AuditAction.DELETE, "Xoa thang diem chi tieu DGRR khac");
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_criteria_other_scale", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Thang điểm chỉ tiêu đánh giá rủi ro HO, CNTT, Dự án, Dịch vụ thuê ngoài", exportColumns(), exportRows());
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
        Map<String, UUID> criteriaIdsByCode = new HashMap<>();
        criteriaOtherRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> criteriaIdsByCode.put(c.getCode(), c.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String auditObjectCategoryCode = row.get("auditObjectCategoryCode");
                String criteriaOtherCode = row.get("criteriaOtherCode");
                Integer scaleScore = parseInt(row.get("scaleScore"));
                String ratingLevel = row.get("ratingLevel");
                if (isBlank(auditObjectCategoryCode) || isBlank(criteriaOtherCode) || scaleScore == null || isBlank(ratingLevel)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Loai doi tuong KT, Ma chi tieu, Thang diem hoac Muc do danh gia");
                }
                UUID auditObjectCategoryId = categoryIdsByCode.get(auditObjectCategoryCode.trim());
                if (auditObjectCategoryId == null) {
                    throw new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan: " + auditObjectCategoryCode);
                }
                UUID criteriaOtherId = criteriaIdsByCode.get(criteriaOtherCode.trim());
                if (criteriaOtherId == null) {
                    throw new BusinessException("RISK_CRITERIA_OTHER_NOT_FOUND", "Khong tim thay chi tieu: " + criteriaOtherCode);
                }
                create(new RiskCriteriaOtherScaleRequest(auditObjectCategoryId, criteriaOtherId, scaleScore,
                        ratingLevel.trim(), emptyToNull(row.get("description")), true));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskCriteriaOtherScale", null, AuditAction.CREATE,
                "Import Excel thang diem chi tieu DGRR khac: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(RiskCriteriaOtherScale item, RiskCriteriaOtherScaleRequest request) {
        item.setAuditObjectCategoryId(request.auditObjectCategoryId());
        item.setCriteriaOtherId(request.criteriaOtherId());
        item.setScaleScore(request.scaleScore());
        item.setRatingLevel(request.ratingLevel());
        item.setDescription(request.description());
        item.setActive(request.active());
    }

    private void validateAuditObjectCategory(UUID tenantId, UUID auditObjectCategoryId) {
        auditObjectCategoryRepository.findById(auditObjectCategoryId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan"));
    }

    private void validateCriteriaOther(UUID tenantId, UUID criteriaOtherId) {
        criteriaOtherRepository.findById(criteriaOtherId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_CRITERIA_OTHER_NOT_FOUND", "Khong tim thay chi tieu danh gia rui ro khac"));
    }

    private RiskCriteriaOtherScale getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_CRITERIA_OTHER_SCALE_NOT_FOUND", "Khong tim thay thang diem chi tieu", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditObjectCategory> categoriesById(UUID tenantId) {
        Map<UUID, AuditObjectCategory> map = new HashMap<>();
        for (AuditObjectCategory c : auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private Map<UUID, RiskCriteriaOther> criteriaById(UUID tenantId) {
        Map<UUID, RiskCriteriaOther> map = new HashMap<>();
        for (RiskCriteriaOther c : criteriaOtherRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("auditObjectCategoryCode", "Loai doi tuong KT"),
                new ExportColumn("criteriaOtherCode", "Ma chi tieu"),
                new ExportColumn("scaleScore", "Thang diem"),
                new ExportColumn("ratingLevel", "Muc do danh gia"),
                new ExportColumn("description", "Dien giai"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditObjectCategory> categories = categoriesById(tenantId);
        Map<UUID, RiskCriteriaOther> criteria = criteriaById(tenantId);
        return repository.findByTenantIdOrderByCriteriaOtherIdAscScaleScoreAsc(tenantId).stream()
                .map(item -> {
                    AuditObjectCategory category = categories.get(item.getAuditObjectCategoryId());
                    RiskCriteriaOther criterion = criteria.get(item.getCriteriaOtherId());
                    Map<String, Object> row = new HashMap<>();
                    row.put("auditObjectCategoryCode", category != null ? category.getCode() : null);
                    row.put("criteriaOtherCode", criterion != null ? criterion.getCode() : null);
                    row.put("scaleScore", item.getScaleScore());
                    row.put("ratingLevel", item.getRatingLevel());
                    row.put("description", item.getDescription());
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

    private RiskCriteriaOtherScaleResponse toResponse(RiskCriteriaOtherScale item, Map<UUID, AuditObjectCategory> categories,
                                                        Map<UUID, RiskCriteriaOther> criteria) {
        AuditObjectCategory category = categories.get(item.getAuditObjectCategoryId());
        RiskCriteriaOther criterion = criteria.get(item.getCriteriaOtherId());
        return new RiskCriteriaOtherScaleResponse(item.getId(),
                item.getAuditObjectCategoryId(), category != null ? category.getCode() : null, category != null ? category.getName() : null,
                item.getCriteriaOtherId(), criterion != null ? criterion.getCode() : null, criterion != null ? criterion.getName() : null,
                item.getScaleScore(), item.getRatingLevel(), item.getDescription(), item.isActive());
    }
}
