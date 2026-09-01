package com.govia.audit.exceptiontype.service;

import com.govia.audit.exceptiontype.dto.AuditExceptionTypeRequest;
import com.govia.audit.exceptiontype.dto.AuditExceptionTypeResponse;
import com.govia.audit.exceptiontype.entity.AuditExceptionCategory;
import com.govia.audit.exceptiontype.entity.AuditExceptionType;
import com.govia.audit.exceptiontype.repository.AuditExceptionTypeRepository;
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

/** CRUD + Import/Export cho danh muc "Loai ton tai sai sot" (sheet ZTC_TTSS). */
@Service
public class AuditExceptionTypeService {

    private final AuditExceptionTypeRepository repository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditExceptionTypeService(AuditExceptionTypeRepository repository, AuditMasterDataItemRepository masterDataItemRepository,
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
    public List<AuditExceptionTypeResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream().map(item -> toResponse(item, segments)).toList();
    }

    @Transactional
    public AuditExceptionTypeResponse create(AuditExceptionTypeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);
        validateBusinessSegment(tenantId, request.businessSegmentId());

        AuditExceptionType item = new AuditExceptionType();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditExceptionType", item.getId(), AuditAction.CREATE, "Tao loai ton tai sai sot: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId));
    }

    @Transactional
    public AuditExceptionTypeResponse update(UUID id, AuditExceptionTypeRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditExceptionType item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);
        validateBusinessSegment(tenantId, request.businessSegmentId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditExceptionType", item.getId(), AuditAction.UPDATE, "Cap nhat loai ton tai sai sot: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditExceptionType item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditExceptionType", id, AuditAction.DELETE, "Xoa loai ton tai sai sot: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_exception_type", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Loại tồn tại sai sót", exportColumns(), exportRows());
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

                Optional<AuditExceptionType> existing = repository.findByTenantIdAndCode(tenantId, code.trim());
                AuditExceptionTypeRequest request = new AuditExceptionTypeRequest(businessSegmentId, code.trim(), name.trim(),
                        parseEnum(AuditExceptionCategory.class, row.get("category")), parseEnum(AuditLevel.class, row.get("impactLevel")),
                        emptyToNull(row.get("classificationBasis")), existing.map(AuditExceptionType::isActive).orElse(true));
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

        auditLogService.record("AuditExceptionType", null, AuditAction.CREATE,
                "Import Excel loai ton tai sai sot: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditExceptionType item, AuditExceptionTypeRequest request) {
        item.setBusinessSegmentId(request.businessSegmentId());
        item.setCode(request.code());
        item.setName(request.name());
        item.setCategory(request.category());
        item.setImpactLevel(request.impactLevel());
        item.setClassificationBasis(request.classificationBasis());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_EXCEPTION_TYPE_CODE_DUPLICATE", "Ma phat hien da ton tai: " + code);
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

    private AuditExceptionType getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_EXCEPTION_TYPE_NOT_FOUND", "Khong tim thay loai ton tai sai sot", HttpStatus.NOT_FOUND));
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
                new ExportColumn("classificationBasis", "Căn cứ phân loại"));
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

    private AuditExceptionTypeResponse toResponse(AuditExceptionType item, Map<UUID, AuditMasterDataItem> segments) {
        AuditMasterDataItem segment = item.getBusinessSegmentId() == null ? null : segments.get(item.getBusinessSegmentId());
        return new AuditExceptionTypeResponse(item.getId(), item.getBusinessSegmentId(),
                segment == null ? null : segment.getCode(), segment == null ? null : segment.getName(),
                item.getCode(), item.getName(), item.getCategory(), item.getImpactLevel(), item.getClassificationBasis(), item.isActive());
    }
}
