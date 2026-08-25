package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.CriteriaQualitativeRequest;
import com.govia.audit.riskscoring.masterdata.dto.CriteriaQualitativeResponse;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectType;
import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQualitative;
import com.govia.audit.riskscoring.masterdata.entity.RiskGroup1;
import com.govia.audit.riskscoring.masterdata.entity.RiskGroup2;
import com.govia.audit.riskscoring.masterdata.repository.RiskCriteriaQualitativeRepository;
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

/** CRUD + Import/Export cho danh muc "Chi tieu danh gia rui ro dinh tinh" (sheet ZTC_CTDGRR_DT). */
@Service
public class CriteriaQualitativeService {

    private final RiskCriteriaQualitativeRepository repository;
    private final RiskGroup1Repository group1Repository;
    private final RiskGroup2Repository group2Repository;
    private final AuditObjectReferenceService auditObjectReferenceService;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public CriteriaQualitativeService(RiskCriteriaQualitativeRepository repository, RiskGroup1Repository group1Repository,
                                       RiskGroup2Repository group2Repository, AuditObjectReferenceService auditObjectReferenceService,
                                       AuditLogService auditLogService, ExcelExportService excelExportService,
                                       WordExportService wordExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.group1Repository = group1Repository;
        this.group2Repository = group2Repository;
        this.auditObjectReferenceService = auditObjectReferenceService;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<CriteriaQualitativeResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, String> group1Codes = group1CodesById(tenantId);
        Map<UUID, String> group2Codes = group2CodesById(tenantId);
        Map<AuditObjectType, Map<UUID, AuditObjectReferenceService.Ref>> auditObjectRefs = auditObjectReferenceService.loadAllRefs(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> toResponse(item, group1Codes, group2Codes, auditObjectRefs)).toList();
    }

    @Transactional
    public CriteriaQualitativeResponse create(CriteriaQualitativeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);
        validateGroups(tenantId, request.group1Id(), request.group2Id());
        auditObjectReferenceService.validateExists(tenantId, request.auditObjectType(), request.auditObjectId());

        RiskCriteriaQualitative item = new RiskCriteriaQualitative();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaQualitative", item.getId(), AuditAction.CREATE, "Tao chi tieu dinh tinh: " + item.getCode());
        return toResponse(item, group1CodesById(tenantId), group2CodesById(tenantId), auditObjectReferenceService.loadAllRefs(tenantId));
    }

    @Transactional
    public CriteriaQualitativeResponse update(UUID id, CriteriaQualitativeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaQualitative item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);
        validateGroups(tenantId, request.group1Id(), request.group2Id());
        auditObjectReferenceService.validateExists(tenantId, request.auditObjectType(), request.auditObjectId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskCriteriaQualitative", item.getId(), AuditAction.UPDATE, "Cap nhat chi tieu dinh tinh: " + item.getCode());
        return toResponse(item, group1CodesById(tenantId), group2CodesById(tenantId), auditObjectReferenceService.loadAllRefs(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskCriteriaQualitative item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskCriteriaQualitative", id, AuditAction.DELETE, "Xoa chi tieu dinh tinh: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_criteria_qualitative", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Chỉ tiêu đánh giá rủi ro định tính", exportColumns(), exportRows());
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
                String auditObjectTypeStr = row.get("auditObjectType");
                String auditObjectCode = row.get("auditObjectCode");
                String group1Code = row.get("group1Code");
                String code = row.get("code");
                String name = row.get("name");
                if (isBlank(auditObjectTypeStr) || isBlank(auditObjectCode) || isBlank(group1Code) || isBlank(code) || isBlank(name)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Loai doi tuong, Ma doi tuong kiem toan, Ma nhom cap 1, Ma hoac Ten");
                }
                UUID group1Id = group1IdsByCode.get(group1Code.trim());
                if (group1Id == null) {
                    throw new BusinessException("RISK_GROUP1_NOT_FOUND", "Khong tim thay nhom cap 1: " + group1Code);
                }
                AuditObjectType auditObjectType = AuditObjectType.valueOf(auditObjectTypeStr.trim());
                UUID auditObjectId = auditObjectReferenceService.resolveIdByCode(tenantId, auditObjectType, auditObjectCode.trim());
                if (auditObjectId == null) {
                    throw new BusinessException("AUDIT_OBJECT_REFERENCE_NOT_FOUND", "Khong tim thay doi tuong kiem toan: " + auditObjectCode);
                }
                String group2Code = row.get("group2Code");
                UUID group2Id = isBlank(group2Code) ? null : group2IdsByCode.get(group2Code.trim());
                create(new CriteriaQualitativeRequest(auditObjectType, auditObjectId, group1Id, group2Id,
                        code.trim(), name.trim(), parseDecimal(row.get("weight")), parseInt(row.get("impactLevel")),
                        parseInt(row.get("likelihoodLevel")), true, true));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskCriteriaQualitative", null, AuditAction.CREATE,
                "Import Excel chi tieu dinh tinh: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(RiskCriteriaQualitative item, CriteriaQualitativeRequest request) {
        item.setAuditObjectType(request.auditObjectType());
        item.setAuditObjectId(request.auditObjectId());
        item.setGroup1Id(request.group1Id());
        item.setGroup2Id(request.group2Id());
        item.setCode(request.code());
        item.setName(request.name());
        item.setWeight(request.weight());
        item.setImpactLevel(request.impactLevel());
        item.setLikelihoodLevel(request.likelihoodLevel());
        item.setIncludeCurrentYear(request.includeCurrentYear());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_CRITERIA_DT_CODE_DUPLICATE", "Ma chi tieu da ton tai: " + code);
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

    private RiskCriteriaQualitative getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_CRITERIA_DT_NOT_FOUND", "Khong tim thay chi tieu dinh tinh", HttpStatus.NOT_FOUND));
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

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("auditObjectType", "Loai doi tuong"),
                new ExportColumn("auditObjectCode", "Ma doi tuong kiem toan"),
                new ExportColumn("group1Code", "Nhom cap 1"),
                new ExportColumn("group2Code", "Nhom cap 2"),
                new ExportColumn("code", "Ma chi tieu"),
                new ExportColumn("name", "Ten chi tieu"),
                new ExportColumn("weight", "Trong so"),
                new ExportColumn("impactLevel", "Muc do RR anh huong"),
                new ExportColumn("likelihoodLevel", "Kha nang xay ra"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, String> group1Codes = group1CodesById(tenantId);
        Map<UUID, String> group2Codes = group2CodesById(tenantId);
        Map<AuditObjectType, Map<UUID, AuditObjectReferenceService.Ref>> auditObjectRefs = auditObjectReferenceService.loadAllRefs(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> {
                    AuditObjectReferenceService.Ref ref = auditObjectReferenceService.lookup(auditObjectRefs, item.getAuditObjectType(), item.getAuditObjectId());
                    Map<String, Object> row = new HashMap<>();
                    row.put("auditObjectType", item.getAuditObjectType());
                    row.put("auditObjectCode", ref != null ? ref.code() : null);
                    row.put("group1Code", group1Codes.get(item.getGroup1Id()));
                    row.put("group2Code", group2Codes.get(item.getGroup2Id()));
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("weight", item.getWeight());
                    row.put("impactLevel", item.getImpactLevel());
                    row.put("likelihoodLevel", item.getLikelihoodLevel());
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

    private CriteriaQualitativeResponse toResponse(RiskCriteriaQualitative item, Map<UUID, String> group1Codes, Map<UUID, String> group2Codes,
                                                     Map<AuditObjectType, Map<UUID, AuditObjectReferenceService.Ref>> auditObjectRefs) {
        AuditObjectReferenceService.Ref ref = auditObjectReferenceService.lookup(auditObjectRefs, item.getAuditObjectType(), item.getAuditObjectId());
        return new CriteriaQualitativeResponse(item.getId(), item.getAuditObjectType().name(), item.getAuditObjectId(),
                ref != null ? ref.code() : null, ref != null ? ref.name() : null,
                item.getGroup1Id(), group1Codes.get(item.getGroup1Id()), item.getGroup2Id(), group2Codes.get(item.getGroup2Id()),
                item.getCode(), item.getName(), item.getWeight(), item.getImpactLevel(), item.getLikelihoodLevel(),
                item.isIncludeCurrentYear(), item.isActive());
    }
}
