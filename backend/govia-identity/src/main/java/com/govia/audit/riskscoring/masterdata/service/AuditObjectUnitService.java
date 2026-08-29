package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.riskscoring.masterdata.dto.AuditObjectUnitRequest;
import com.govia.audit.riskscoring.masterdata.dto.AuditObjectUnitResponse;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectCategoryRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.riskscoring.scoring.entity.RiskGroupHO;
import com.govia.audit.riskscoring.scoring.repository.RiskGroupHORepository;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD + Import/Export cho danh muc "Doi tuong kiem toan HO, Giam sat CC, Chi nhanh" (sheet
 * ZTC_DTKT1). "Thuoc tuyen bao ve" link sang RiskGroupHO cua sub-module Cham Diem (sheet
 * ZTC_Nhom_DGRR_HO) - tai su dung catalog da co thay vi nhap tay.
 */
@Service
public class AuditObjectUnitService {

    private final AuditObjectUnitRepository repository;
    private final RiskGroupHORepository groupHORepository;
    private final AuditObjectCategoryRepository auditObjectCategoryRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditObjectUnitService(AuditObjectUnitRepository repository, RiskGroupHORepository groupHORepository,
                                   AuditObjectCategoryRepository auditObjectCategoryRepository,
                                   AuditMasterDataItemRepository masterDataItemRepository, AuditLogService auditLogService,
                                   ExcelExportService excelExportService, WordExportService wordExportService,
                                   ExcelImportService excelImportService) {
        this.repository = repository;
        this.groupHORepository = groupHORepository;
        this.auditObjectCategoryRepository = auditObjectCategoryRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditObjectUnitResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, String> groupCodes = groupHOCodesById(tenantId);
        Map<UUID, String> categoryCodes = auditObjectCategoryCodesById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream().map(item -> toResponse(item, groupCodes, categoryCodes)).toList();
    }

    @Transactional
    public AuditObjectUnitResponse create(AuditObjectUnitRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);
        validateDefenseLineGroup(tenantId, request.defenseLineGroupId());
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());
        validateUnitType(tenantId, request.unitType());

        AuditObjectUnit item = new AuditObjectUnit();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item.setInfoUpdatedDate(LocalDate.now());
        item = repository.save(item);

        auditLogService.record("AuditObjectUnit", item.getId(), AuditAction.CREATE, "Tao doi tuong kiem toan: " + item.getCode());
        return toResponse(item, groupHOCodesById(tenantId), auditObjectCategoryCodesById(tenantId));
    }

    @Transactional
    public AuditObjectUnitResponse update(UUID id, AuditObjectUnitRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectUnit item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);
        validateDefenseLineGroup(tenantId, request.defenseLineGroupId());
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());
        validateUnitType(tenantId, request.unitType());

        applyRequest(item, request);
        item.setInfoUpdatedDate(LocalDate.now());
        item = repository.save(item);

        auditLogService.record("AuditObjectUnit", item.getId(), AuditAction.UPDATE, "Cap nhat doi tuong kiem toan: " + item.getCode());
        return toResponse(item, groupHOCodesById(tenantId), auditObjectCategoryCodesById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectUnit item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditObjectUnit", id, AuditAction.DELETE, "Xoa doi tuong kiem toan: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_audit_object_unit", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Đối tượng kiểm toán HO, Giám sát CC, Chi nhánh", exportColumns(), exportRows());
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
        Map<String, UUID> groupIdsByCode = new HashMap<>();
        groupHORepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(g -> groupIdsByCode.put(g.getCode(), g.getId()));
        Map<String, UUID> categoryIdsByCode = new HashMap<>();
        auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> categoryIdsByCode.put(c.getCode(), c.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String code = row.get("code");
                String name = row.get("name");
                String unitType = row.get("unitType");
                if (isBlank(code) || isBlank(name) || isBlank(unitType)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma don vi, Ten don vi hoac Loai don vi");
                }
                String defenseLineGroupCode = row.get("defenseLineGroupCode");
                UUID defenseLineGroupId = isBlank(defenseLineGroupCode) ? null : groupIdsByCode.get(defenseLineGroupCode.trim());
                String auditObjectCategoryCode = row.get("auditObjectCategoryCode");
                UUID auditObjectCategoryId = isBlank(auditObjectCategoryCode) ? null : categoryIdsByCode.get(auditObjectCategoryCode.trim());
                create(new AuditObjectUnitRequest(code.trim(), name.trim(), unitType.trim(), auditObjectCategoryId,
                        parseDate(row.get("establishedDate")), parseDate(row.get("restructureDate")),
                        emptyToNull(row.get("restructureNote")), parseInt(row.get("totalStaff")), parseInt(row.get("leaderCount")),
                        parseInt(row.get("staffCount")), parseInt(row.get("rankValue")), defenseLineGroupId,
                        emptyToNull(row.get("operatingRegulation")), emptyToNull(row.get("mainFunction")),
                        emptyToNull(row.get("keyFindings")), true));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditObjectUnit", null, AuditAction.CREATE,
                "Import Excel doi tuong kiem toan: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditObjectUnit item, AuditObjectUnitRequest request) {
        item.setCode(request.code());
        item.setName(request.name());
        item.setUnitType(request.unitType());
        item.setAuditObjectCategoryId(request.auditObjectCategoryId());
        item.setEstablishedDate(request.establishedDate());
        item.setRestructureDate(request.restructureDate());
        item.setRestructureNote(request.restructureNote());
        item.setTotalStaff(request.totalStaff());
        item.setLeaderCount(request.leaderCount());
        item.setStaffCount(request.staffCount());
        item.setRankValue(request.rankValue());
        item.setDefenseLineGroupId(request.defenseLineGroupId());
        item.setOperatingRegulation(request.operatingRegulation());
        item.setMainFunction(request.mainFunction());
        item.setKeyFindings(request.keyFindings());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_OBJECT_UNIT_CODE_DUPLICATE", "Ma don vi da ton tai: " + code);
                });
    }

    private void validateDefenseLineGroup(UUID tenantId, UUID defenseLineGroupId) {
        if (defenseLineGroupId == null) {
            return;
        }
        groupHORepository.findById(defenseLineGroupId)
                .filter(g -> g.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_GROUP_HO_NOT_FOUND", "Khong tim thay nhom rui ro HO (tuyen bao ve)"));
    }

    private void validateUnitType(UUID tenantId, String unitType) {
        masterDataItemRepository.findByTenantIdAndCategoryAndCode(tenantId, AuditMasterDataCategory.UNIT_TYPE, unitType)
                .orElseThrow(() -> new BusinessException("UNIT_TYPE_NOT_FOUND", "Khong tim thay loai don vi: " + unitType));
    }

    private void validateAuditObjectCategory(UUID tenantId, UUID auditObjectCategoryId) {
        if (auditObjectCategoryId == null) {
            return;
        }
        auditObjectCategoryRepository.findById(auditObjectCategoryId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan"));
    }

    private AuditObjectUnit getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_UNIT_NOT_FOUND", "Khong tim thay doi tuong kiem toan", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, String> groupHOCodesById(UUID tenantId) {
        Map<UUID, String> map = new HashMap<>();
        for (RiskGroupHO g : groupHORepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(g.getId(), g.getCode());
        }
        return map;
    }

    private Map<UUID, String> auditObjectCategoryCodesById(UUID tenantId) {
        Map<UUID, String> map = new HashMap<>();
        for (AuditObjectCategory c : auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(c.getId(), c.getCode());
        }
        return map;
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("code", "Ma don vi"),
                new ExportColumn("name", "Ten don vi"),
                new ExportColumn("unitType", "Loai don vi"),
                new ExportColumn("auditObjectCategoryCode", "Loai doi tuong kiem toan"),
                new ExportColumn("establishedDate", "Ngay thanh lap"),
                new ExportColumn("restructureDate", "Ngay chia tach/sap nhap"),
                new ExportColumn("restructureNote", "Ghi chu chia tach/sap nhap"),
                new ExportColumn("totalStaff", "Tong so can bo"),
                new ExportColumn("leaderCount", "So luong lanh dao"),
                new ExportColumn("staffCount", "So luong nhan vien"),
                new ExportColumn("rankValue", "Xep hang"),
                new ExportColumn("defenseLineGroupCode", "Thuoc tuyen bao ve"),
                new ExportColumn("operatingRegulation", "Quy che to chuc hoat dong"),
                new ExportColumn("mainFunction", "Chuc nang nhiem vu chinh"),
                new ExportColumn("keyFindings", "Phat hien trong yeu"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, String> groupCodes = groupHOCodesById(tenantId);
        Map<UUID, String> categoryCodes = auditObjectCategoryCodesById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("unitType", item.getUnitType());
                    row.put("auditObjectCategoryCode", categoryCodes.get(item.getAuditObjectCategoryId()));
                    row.put("establishedDate", item.getEstablishedDate());
                    row.put("restructureDate", item.getRestructureDate());
                    row.put("restructureNote", item.getRestructureNote());
                    row.put("totalStaff", item.getTotalStaff());
                    row.put("leaderCount", item.getLeaderCount());
                    row.put("staffCount", item.getStaffCount());
                    row.put("rankValue", item.getRankValue());
                    row.put("defenseLineGroupCode", groupCodes.get(item.getDefenseLineGroupId()));
                    row.put("operatingRegulation", item.getOperatingRegulation());
                    row.put("mainFunction", item.getMainFunction());
                    row.put("keyFindings", item.getKeyFindings());
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

    private AuditObjectUnitResponse toResponse(AuditObjectUnit item, Map<UUID, String> groupCodes, Map<UUID, String> categoryCodes) {
        return new AuditObjectUnitResponse(item.getId(), item.getCode(), item.getName(), item.getUnitType(),
                item.getAuditObjectCategoryId(), categoryCodes.get(item.getAuditObjectCategoryId()),
                item.getEstablishedDate(), item.getRestructureDate(), item.getRestructureNote(), item.getTotalStaff(),
                item.getLeaderCount(), item.getStaffCount(), item.getRankValue(), item.getDefenseLineGroupId(),
                groupCodes.get(item.getDefenseLineGroupId()), item.getOperatingRegulation(), item.getMainFunction(),
                item.getKeyFindings(), item.getInfoUpdatedDate(), item.isActive());
    }
}
