package com.govia.audit.workitemqt.service;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectCategoryRepository;
import com.govia.audit.workitem.entity.AuditWorkPhase;
import com.govia.audit.workitemqt.dto.AuditWorkItemQtRequest;
import com.govia.audit.workitemqt.dto.AuditWorkItemQtResponse;
import com.govia.audit.workitemqt.entity.AuditWorkItemQt;
import com.govia.audit.workitemqt.repository.AuditWorkItemQtRepository;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** CRUD + Import/Export cho danh muc "Bang ma cong viec quy trinh" (sheet ZTC_CV_QT). */
@Service
public class AuditWorkItemQtService {

    private final AuditWorkItemQtRepository repository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditObjectCategoryRepository auditObjectCategoryRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditWorkItemQtService(AuditWorkItemQtRepository repository, AuditMasterDataItemRepository masterDataItemRepository,
                                   AuditObjectCategoryRepository auditObjectCategoryRepository, AuditLogService auditLogService,
                                   ExcelExportService excelExportService, WordExportService wordExportService,
                                   ExcelImportService excelImportService) {
        this.repository = repository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.auditObjectCategoryRepository = auditObjectCategoryRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditWorkItemQtResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        Map<UUID, AuditObjectCategory> objectCategories = objectCategoriesById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream().map(item -> toResponse(item, segments, objectCategories)).toList();
    }

    @Transactional
    public AuditWorkItemQtResponse create(AuditWorkItemQtRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), request.applicableYear(), request.workSetCode(), null);
        validateBusinessSegment(tenantId, request.businessSegmentId());
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());

        AuditWorkItemQt item = new AuditWorkItemQt();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditWorkItemQt", item.getId(), AuditAction.CREATE, "Tao cong viec quy trinh: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId), objectCategoriesById(tenantId));
    }

    @Transactional
    public AuditWorkItemQtResponse update(UUID id, AuditWorkItemQtRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditWorkItemQt item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), request.applicableYear(), request.workSetCode(), id);
        validateBusinessSegment(tenantId, request.businessSegmentId());
        validateAuditObjectCategory(tenantId, request.auditObjectCategoryId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditWorkItemQt", item.getId(), AuditAction.UPDATE, "Cap nhat cong viec quy trinh: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId), objectCategoriesById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditWorkItemQt item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditWorkItemQt", id, AuditAction.DELETE, "Xoa cong viec quy trinh: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_work_item_qt", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Bảng mã công việc quy trình", exportColumns(), exportRows());
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
        Map<String, UUID> segmentIdsByCode = new HashMap<>();
        masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT)
                .forEach(s -> segmentIdsByCode.put(s.getCode(), s.getId()));
        Map<String, UUID> objectCategoryIdsByCode = new HashMap<>();
        auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> objectCategoryIdsByCode.put(c.getCode(), c.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String code = row.get("code");
                String name = row.get("name");
                Integer applicableYear = parseInt(row.get("applicableYear"));
                String workSetCode = emptyToNull(row.get("workSetCode"));
                if (isBlank(code) || isBlank(name)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma cong viec hoac Ten cong viec");
                }
                if (applicableYear == null || workSetCode == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Nam hoac Ma bo cong viec (bat buoc dien)");
                }
                String segmentCode = row.get("businessSegmentCode");
                UUID businessSegmentId = isBlank(segmentCode) ? null : segmentIdsByCode.get(segmentCode.trim());
                String objectCategoryCode = row.get("auditObjectCategoryCode");
                UUID auditObjectCategoryId = isBlank(objectCategoryCode) ? null : objectCategoryIdsByCode.get(objectCategoryCode.trim());

                Optional<AuditWorkItemQt> existing = repository.findByTenantIdAndCodeAndApplicableYearAndWorkSetCode(
                        tenantId, code.trim(), applicableYear, workSetCode);
                AuditWorkItemQtRequest request = new AuditWorkItemQtRequest(auditObjectCategoryId,
                        parseEnum(AuditWorkPhase.class, row.get("phase")), businessSegmentId, code.trim(),
                        name.trim(), applicableYear, workSetCode, emptyToNull(row.get("workType")),
                        existing.map(AuditWorkItemQt::isActive).orElse(true), parseBoolean(row.get("hasSampleSelection")),
                        emptyToNull(row.get("branchOrHeadOffice")));
                if (existing.isPresent()) {
                    update(existing.get().getId(), request);
                } else {
                    create(request);
                }
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditWorkItemQt", null, AuditAction.CREATE,
                "Import Excel cong viec quy trinh: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditWorkItemQt item, AuditWorkItemQtRequest request) {
        item.setAuditObjectCategoryId(request.auditObjectCategoryId());
        item.setPhase(request.phase());
        item.setBusinessSegmentId(request.businessSegmentId());
        item.setCode(request.code());
        item.setName(request.name());
        item.setApplicableYear(request.applicableYear());
        item.setWorkSetCode(request.workSetCode());
        item.setWorkType(request.workType());
        item.setActive(request.active());
        item.setHasSampleSelection(request.hasSampleSelection());
        item.setBranchOrHeadOffice(request.branchOrHeadOffice());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, Integer applicableYear, String workSetCode, UUID excludingId) {
        repository.findByTenantIdAndCodeAndApplicableYearAndWorkSetCode(tenantId, code, applicableYear, workSetCode)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_WORK_ITEM_QT_CODE_DUPLICATE",
                            "Ma cong viec da ton tai cho nam " + applicableYear + " / bo cong viec " + workSetCode + ": " + code);
                });
    }

    private void validateBusinessSegment(UUID tenantId, UUID businessSegmentId) {
        if (businessSegmentId == null) {
            return;
        }
        masterDataItemRepository.findById(businessSegmentId)
                .filter(item -> item.getTenantId().equals(tenantId) && item.getCategory() == AuditMasterDataCategory.BUSINESS_SEGMENT)
                .orElseThrow(() -> new BusinessException("BUSINESS_SEGMENT_NOT_FOUND", "Khong tim thay mang nghiep vu"));
    }

    private void validateAuditObjectCategory(UUID tenantId, UUID auditObjectCategoryId) {
        if (auditObjectCategoryId == null) {
            return;
        }
        auditObjectCategoryRepository.findById(auditObjectCategoryId)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_CATEGORY_NOT_FOUND", "Khong tim thay loai doi tuong kiem toan"));
    }

    private AuditWorkItemQt getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_WORK_ITEM_QT_NOT_FOUND", "Khong tim thay cong viec quy trinh", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> businessSegmentsById(UUID tenantId) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private Map<UUID, AuditObjectCategory> objectCategoriesById(UUID tenantId) {
        return auditObjectCategoryRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditObjectCategory::getId, i -> i));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("auditObjectCategoryCode", "Loại đối tượng"),
                new ExportColumn("phase", "Giai đoạn"),
                new ExportColumn("businessSegmentCode", "Mảng nghiệp vụ"),
                new ExportColumn("code", "Mã công việc"),
                new ExportColumn("name", "Tên công việc"),
                new ExportColumn("applicableYear", "Năm"),
                new ExportColumn("workSetCode", "Mã bộ công việc"),
                new ExportColumn("workType", "Loại CV"),
                new ExportColumn("hasSampleSelection", "Có chọn mẫu"),
                new ExportColumn("branchOrHeadOffice", "CN hoặc HO"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        Map<UUID, AuditObjectCategory> objectCategories = objectCategoriesById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    AuditObjectCategory objectCategory = objectCategories.get(item.getAuditObjectCategoryId());
                    row.put("auditObjectCategoryCode", objectCategory == null ? null : objectCategory.getCode());
                    row.put("phase", item.getPhase());
                    row.put("businessSegmentCode", codeOf(segments.get(item.getBusinessSegmentId())));
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("applicableYear", item.getApplicableYear());
                    row.put("workSetCode", item.getWorkSetCode());
                    row.put("workType", item.getWorkType());
                    row.put("hasSampleSelection", item.isHasSampleSelection() ? "Y" : "N");
                    row.put("branchOrHeadOffice", item.getBranchOrHeadOffice());
                    return row;
                }).toList();
    }

    private String codeOf(AuditMasterDataItem item) {
        return item == null ? null : item.getCode();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "Y".equalsIgnoreCase(value);
    }

    private Integer parseInt(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private AuditWorkItemQtResponse toResponse(AuditWorkItemQt item, Map<UUID, AuditMasterDataItem> segments,
                                                Map<UUID, AuditObjectCategory> objectCategories) {
        AuditMasterDataItem segment = item.getBusinessSegmentId() == null ? null : segments.get(item.getBusinessSegmentId());
        AuditObjectCategory objectCategory = item.getAuditObjectCategoryId() == null ? null : objectCategories.get(item.getAuditObjectCategoryId());
        return new AuditWorkItemQtResponse(item.getId(),
                item.getAuditObjectCategoryId(), objectCategory == null ? null : objectCategory.getCode(),
                objectCategory == null ? null : objectCategory.getName(),
                item.getPhase(), item.getBusinessSegmentId(),
                segment == null ? null : segment.getCode(), segment == null ? null : segment.getName(),
                item.getCode(), item.getName(), item.getApplicableYear(), item.getWorkSetCode(), item.getWorkType(),
                item.isActive(), item.isHasSampleSelection(), item.getBranchOrHeadOffice());
    }
}
