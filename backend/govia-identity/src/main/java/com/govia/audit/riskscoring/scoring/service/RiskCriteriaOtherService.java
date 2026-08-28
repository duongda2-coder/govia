package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectCategoryRepository;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaOtherRequest;
import com.govia.audit.riskscoring.scoring.dto.RiskCriteriaOtherResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaOther;
import com.govia.audit.riskscoring.scoring.entity.RiskGroupHO;
import com.govia.audit.riskscoring.scoring.entity.RiskTypeHO;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaOtherRepository;
import com.govia.audit.riskscoring.scoring.repository.RiskGroupHORepository;
import com.govia.audit.riskscoring.scoring.repository.RiskTypeHORepository;
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

/**
 * CRUD + Import/Export cho danh muc "Chi tieu danh gia rui ro HO, CNTT, Du an, Dich vu thue
 * ngoai..." (sheet ZTC_CTDGRR_KHAC).
 */
@Service
public class RiskCriteriaOtherService {

    private final RiskCriteriaOtherRepository repository;
    private final AuditObjectCategoryRepository auditObjectCategoryRepository;
    private final RiskGroupHORepository groupHoRepository;
    private final RiskTypeHORepository riskTypeHoRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public RiskCriteriaOtherService(RiskCriteriaOtherRepository repository,
                                     AuditObjectCategoryRepository auditObjectCategoryRepository,
                                     RiskGroupHORepository groupHoRepository,
                                     RiskTypeHORepository riskTypeHoRepository,
                                     AuditLogService auditLogService, ExcelExportService excelExportService,
                                     WordExportService wordExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditObjectCategoryRepository = auditObjectCategoryRepository;
        this.groupHoRepository = groupHoRepository;
        this.riskTypeHoRepository = riskTypeHoRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<RiskCriteriaOtherResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> toResponse(item, categoriesById(tenantId), groupsById(tenantId), typesById(tenantId)))
                .toList();
    }

    @Transactional
    public RiskCriteriaOtherResponse create(RiskCriteriaOtherRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.auditObjectCategoryId(), request.code(), null);
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());
        validateGroupHo(tenantId, request.groupHoId());
        validateRiskTypeHo(tenantId, request.riskTypeHoId());

        RiskCriteriaOther item = new RiskCriteriaOther();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaOther", item.getId(), AuditAction.CREATE, "Tao chi tieu DGRR khac: " + item.getCode());
        return toResponse(item, categoriesById(tenantId), groupsById(tenantId), typesById(tenantId));
    }

    @Transactional
    public RiskCriteriaOtherResponse update(UUID id, RiskCriteriaOtherRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaOther item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.auditObjectCategoryId(), request.code(), id);
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());
        validateGroupHo(tenantId, request.groupHoId());
        validateRiskTypeHo(tenantId, request.riskTypeHoId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaOther", item.getId(), AuditAction.UPDATE, "Cap nhat chi tieu DGRR khac: " + item.getCode());
        return toResponse(item, categoriesById(tenantId), groupsById(tenantId), typesById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaOther item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskCriteriaOther", id, AuditAction.DELETE, "Xoa chi tieu DGRR khac: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_criteria_other", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Chỉ tiêu đánh giá rủi ro HO, CNTT, Dự án, Dịch vụ thuê ngoài", exportColumns(), exportRows());
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
        Map<String, UUID> groupIdsByCode = new HashMap<>();
        groupHoRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(g -> groupIdsByCode.put(g.getCode(), g.getId()));
        Map<String, UUID> typeIdsByCode = new HashMap<>();
        riskTypeHoRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(rt -> typeIdsByCode.put(rt.getCode(), rt.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String auditObjectCategoryCode = row.get("auditObjectCategoryCode");
                String code = row.get("code");
                String name = row.get("name");
                String groupHoCode = row.get("groupHoCode");
                String riskTypeHoCode = row.get("riskTypeHoCode");
                if (isBlank(auditObjectCategoryCode) || isBlank(code) || isBlank(name) || isBlank(groupHoCode) || isBlank(riskTypeHoCode)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Loai doi tuong KT, Ma, Ten, Nhom hoac Loai rui ro HO");
                }
                UUID auditObjectCategoryId = categoryIdsByCode.get(auditObjectCategoryCode.trim());
                if (auditObjectCategoryId == null) {
                    throw new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan: " + auditObjectCategoryCode);
                }
                UUID groupHoId = groupIdsByCode.get(groupHoCode.trim());
                if (groupHoId == null) {
                    throw new BusinessException("RISK_GROUP_HO_NOT_FOUND", "Khong tim thay nhom rui ro HO: " + groupHoCode);
                }
                UUID riskTypeHoId = typeIdsByCode.get(riskTypeHoCode.trim());
                if (riskTypeHoId == null) {
                    throw new BusinessException("RISK_TYPE_HO_NOT_FOUND", "Khong tim thay loai rui ro HO: " + riskTypeHoCode);
                }
                create(new RiskCriteriaOtherRequest(auditObjectCategoryId, code.trim(), name.trim(),
                        parseDecimal(row.get("weight")), groupHoId, riskTypeHoId, true));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskCriteriaOther", null, AuditAction.CREATE,
                "Import Excel chi tieu DGRR khac: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(RiskCriteriaOther item, RiskCriteriaOtherRequest request) {
        item.setAuditObjectCategoryId(request.auditObjectCategoryId());
        item.setCode(request.code());
        item.setName(request.name());
        item.setWeight(request.weight());
        item.setGroupHoId(request.groupHoId());
        item.setRiskTypeHoId(request.riskTypeHoId());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, UUID auditObjectCategoryId, String code, UUID excludingId) {
        repository.findByTenantIdAndAuditObjectCategoryIdAndCode(tenantId, auditObjectCategoryId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_CRITERIA_OTHER_CODE_DUPLICATE", "Ma chi tieu da ton tai: " + code);
                });
    }

    private void validateAuditObjectCategory(UUID tenantId, UUID auditObjectCategoryId) {
        auditObjectCategoryRepository.findById(auditObjectCategoryId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan"));
    }

    private void validateGroupHo(UUID tenantId, UUID groupHoId) {
        groupHoRepository.findById(groupHoId)
                .filter(g -> g.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_GROUP_HO_NOT_FOUND", "Khong tim thay nhom rui ro HO"));
    }

    private void validateRiskTypeHo(UUID tenantId, UUID riskTypeHoId) {
        riskTypeHoRepository.findById(riskTypeHoId)
                .filter(rt -> rt.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_TYPE_HO_NOT_FOUND", "Khong tim thay loai rui ro HO"));
    }

    private RiskCriteriaOther getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_CRITERIA_OTHER_NOT_FOUND", "Khong tim thay chi tieu DGRR khac", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditObjectCategory> categoriesById(UUID tenantId) {
        Map<UUID, AuditObjectCategory> map = new HashMap<>();
        for (AuditObjectCategory c : auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private Map<UUID, RiskGroupHO> groupsById(UUID tenantId) {
        Map<UUID, RiskGroupHO> map = new HashMap<>();
        for (RiskGroupHO g : groupHoRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(g.getId(), g);
        }
        return map;
    }

    private Map<UUID, RiskTypeHO> typesById(UUID tenantId) {
        Map<UUID, RiskTypeHO> map = new HashMap<>();
        for (RiskTypeHO rt : riskTypeHoRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(rt.getId(), rt);
        }
        return map;
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("auditObjectCategoryCode", "Loai doi tuong KT"),
                new ExportColumn("code", "Ma chi tieu"),
                new ExportColumn("name", "Ten chi tieu"),
                new ExportColumn("weight", "Ti trong"),
                new ExportColumn("groupHoCode", "Nhom rui ro HO"),
                new ExportColumn("riskTypeHoCode", "Loai rui ro HO"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditObjectCategory> categories = categoriesById(tenantId);
        Map<UUID, RiskGroupHO> groups = groupsById(tenantId);
        Map<UUID, RiskTypeHO> types = typesById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> {
                    AuditObjectCategory category = categories.get(item.getAuditObjectCategoryId());
                    RiskGroupHO group = groups.get(item.getGroupHoId());
                    RiskTypeHO type = types.get(item.getRiskTypeHoId());
                    Map<String, Object> row = new HashMap<>();
                    row.put("auditObjectCategoryCode", category != null ? category.getCode() : null);
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("weight", item.getWeight());
                    row.put("groupHoCode", group != null ? group.getCode() : null);
                    row.put("riskTypeHoCode", type != null ? type.getCode() : null);
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

    private RiskCriteriaOtherResponse toResponse(RiskCriteriaOther item, Map<UUID, AuditObjectCategory> categories,
                                                  Map<UUID, RiskGroupHO> groups, Map<UUID, RiskTypeHO> types) {
        AuditObjectCategory category = categories.get(item.getAuditObjectCategoryId());
        RiskGroupHO group = groups.get(item.getGroupHoId());
        RiskTypeHO type = types.get(item.getRiskTypeHoId());
        return new RiskCriteriaOtherResponse(item.getId(),
                item.getAuditObjectCategoryId(), category != null ? category.getCode() : null, category != null ? category.getName() : null,
                item.getCode(), item.getName(), item.getWeight(),
                item.getGroupHoId(), group != null ? group.getCode() : null, group != null ? group.getName() : null,
                item.getRiskTypeHoId(), type != null ? type.getCode() : null, type != null ? type.getName() : null,
                item.isActive());
    }
}
