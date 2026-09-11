package com.govia.audit.exceptiontypeqt.service;

import com.govia.audit.exceptiontype.entity.AuditExceptionCategory;
import com.govia.audit.exceptiontypeqt.dto.AuditExceptionTypeQtRequest;
import com.govia.audit.exceptiontypeqt.dto.AuditExceptionTypeQtResponse;
import com.govia.audit.exceptiontypeqt.entity.AuditExceptionTypeQt;
import com.govia.audit.exceptiontypeqt.repository.AuditExceptionTypeQtRepository;
import com.govia.audit.masterdata.entity.AuditLevel;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
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

/** CRUD + Import/Export cho danh muc "Ton tai sai sot quy trinh" (sheet ZTC_TTSS_QT). */
@Service
public class AuditExceptionTypeQtService {

    private final AuditExceptionTypeQtRepository repository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditExceptionTypeQtService(AuditExceptionTypeQtRepository repository, AuditMasterDataItemRepository masterDataItemRepository,
                                        AuditLogService auditLogService, ExcelExportService excelExportService,
                                        WordExportService wordExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditExceptionTypeQtResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream().map(item -> toResponse(item, segments)).toList();
    }

    @Transactional
    public AuditExceptionTypeQtResponse create(AuditExceptionTypeQtRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), request.applicableYear(), null);
        validateBusinessSegment(tenantId, request.businessSegmentId());

        AuditExceptionTypeQt item = new AuditExceptionTypeQt();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditExceptionTypeQt", item.getId(), AuditAction.CREATE, "Tao ton tai sai sot quy trinh: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId));
    }

    @Transactional
    public AuditExceptionTypeQtResponse update(UUID id, AuditExceptionTypeQtRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditExceptionTypeQt item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), request.applicableYear(), id);
        validateBusinessSegment(tenantId, request.businessSegmentId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditExceptionTypeQt", item.getId(), AuditAction.UPDATE, "Cap nhat ton tai sai sot quy trinh: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditExceptionTypeQt item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditExceptionTypeQt", id, AuditAction.DELETE, "Xoa ton tai sai sot quy trinh: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_exception_type_qt", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Tồn tại sai sót quy trình", exportColumns(), exportRows());
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

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String code = row.get("code");
                String name = row.get("name");
                if (isBlank(code) || isBlank(name)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma phat hien hoac Ten phat hien");
                }
                String segmentCode = row.get("businessSegmentCode");
                UUID businessSegmentId = isBlank(segmentCode) ? null : segmentIdsByCode.get(segmentCode.trim());
                Integer applicableYear = parseInt(row.get("applicableYear"));

                Optional<AuditExceptionTypeQt> existing = repository.findByTenantIdAndCodeAndApplicableYear(tenantId, code.trim(), applicableYear);
                AuditExceptionTypeQtRequest request = new AuditExceptionTypeQtRequest(businessSegmentId, code.trim(), name.trim(),
                        parseEnum(AuditExceptionCategory.class, row.get("category")), parseEnum(AuditLevel.class, row.get("impactLevel")),
                        emptyToNull(row.get("classificationBasis")), applicableYear,
                        existing.map(AuditExceptionTypeQt::isActive).orElse(true));
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

        auditLogService.record("AuditExceptionTypeQt", null, AuditAction.CREATE,
                "Import Excel ton tai sai sot quy trinh: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditExceptionTypeQt item, AuditExceptionTypeQtRequest request) {
        item.setBusinessSegmentId(request.businessSegmentId());
        item.setCode(request.code());
        item.setName(request.name());
        item.setCategory(request.category());
        item.setImpactLevel(request.impactLevel());
        item.setClassificationBasis(request.classificationBasis());
        item.setApplicableYear(request.applicableYear());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, Integer applicableYear, UUID excludingId) {
        repository.findByTenantIdAndCodeAndApplicableYear(tenantId, code, applicableYear)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_EXCEPTION_TYPE_QT_CODE_DUPLICATE",
                            "Ma phat hien da ton tai cho nam " + applicableYear + ": " + code);
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

    private AuditExceptionTypeQt getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_EXCEPTION_TYPE_QT_NOT_FOUND", "Khong tim thay ton tai sai sot quy trinh", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> businessSegmentsById(UUID tenantId) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("businessSegmentCode", "Mảng nghiệp vụ"),
                new ExportColumn("code", "Mã phát hiện"),
                new ExportColumn("name", "Tên phát hiện"),
                new ExportColumn("category", "Loại phát hiện"),
                new ExportColumn("impactLevel", "Mức độ ảnh hưởng"),
                new ExportColumn("classificationBasis", "Căn cứ phân loại"),
                new ExportColumn("applicableYear", "Năm"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("businessSegmentCode", codeOf(segments.get(item.getBusinessSegmentId())));
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("category", item.getCategory());
                    row.put("impactLevel", item.getImpactLevel());
                    row.put("classificationBasis", item.getClassificationBasis());
                    row.put("applicableYear", item.getApplicableYear());
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

    private AuditExceptionTypeQtResponse toResponse(AuditExceptionTypeQt item, Map<UUID, AuditMasterDataItem> segments) {
        AuditMasterDataItem segment = item.getBusinessSegmentId() == null ? null : segments.get(item.getBusinessSegmentId());
        return new AuditExceptionTypeQtResponse(item.getId(), item.getBusinessSegmentId(),
                segment == null ? null : segment.getCode(), segment == null ? null : segment.getName(),
                item.getCode(), item.getName(), item.getCategory(), item.getImpactLevel(), item.getClassificationBasis(),
                item.getApplicableYear(), item.isActive());
    }
}
