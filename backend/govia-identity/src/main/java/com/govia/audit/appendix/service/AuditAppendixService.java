package com.govia.audit.appendix.service;

import com.govia.audit.appendix.dto.AuditAppendixRequest;
import com.govia.audit.appendix.dto.AuditAppendixResponse;
import com.govia.audit.appendix.entity.AuditAppendix;
import com.govia.audit.appendix.repository.AuditAppendixRepository;
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
import java.util.UUID;
import java.util.stream.Collectors;

/** CRUD + Import/Export cho danh muc "Quan ly phu luc" (sheet ZTC_phuluc). */
@Service
public class AuditAppendixService {

    private final AuditAppendixRepository repository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditAppendixService(AuditAppendixRepository repository, AuditMasterDataItemRepository masterDataItemRepository,
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
    public List<AuditAppendixResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> businessSegments = businessSegmentsById(tenantId);
        return repository.findByTenantIdOrderByAppendixCodeAsc(tenantId).stream().map(item -> toResponse(item, businessSegments)).toList();
    }

    @Transactional
    public AuditAppendixResponse create(AuditAppendixRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.appendixCode(), null);
        validateBusinessSegment(tenantId, request.businessSegmentId());

        AuditAppendix item = new AuditAppendix();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditAppendix", item.getId(), AuditAction.CREATE, "Tao phu luc: " + item.getAppendixCode());
        return toResponse(item, businessSegmentsById(tenantId));
    }

    @Transactional
    public AuditAppendixResponse update(UUID id, AuditAppendixRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditAppendix item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.appendixCode(), id);
        validateBusinessSegment(tenantId, request.businessSegmentId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditAppendix", item.getId(), AuditAction.UPDATE, "Cap nhat phu luc: " + item.getAppendixCode());
        return toResponse(item, businessSegmentsById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditAppendix item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditAppendix", id, AuditAction.DELETE, "Xoa phu luc: " + item.getAppendixCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_appendix", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Quản lý phụ lục", exportColumns(), exportRows());
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
                String sampleType = row.get("sampleType");
                String appendixCode = row.get("appendixCode");
                if (isBlank(sampleType) || isBlank(appendixCode)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Loai mau chon hoac Ma phu luc");
                }
                String segmentCode = row.get("businessSegmentCode");
                UUID businessSegmentId = isBlank(segmentCode) ? null : segmentIdsByCode.get(segmentCode.trim());
                create(new AuditAppendixRequest(businessSegmentId, sampleType.trim(), appendixCode.trim(),
                        emptyToNull(row.get("note")), true));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditAppendix", null, AuditAction.CREATE,
                "Import Excel phu luc: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditAppendix item, AuditAppendixRequest request) {
        item.setBusinessSegmentId(request.businessSegmentId());
        item.setSampleType(request.sampleType());
        item.setAppendixCode(request.appendixCode());
        item.setNote(request.note());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String appendixCode, UUID excludingId) {
        repository.findByTenantIdAndAppendixCode(tenantId, appendixCode)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_APPENDIX_CODE_DUPLICATE", "Ma phu luc da ton tai: " + appendixCode);
                });
    }

    private void validateBusinessSegment(UUID tenantId, UUID businessSegmentId) {
        if (businessSegmentId == null) {
            return;
        }
        masterDataItemRepository.findById(businessSegmentId)
                .filter(item -> item.getTenantId().equals(tenantId) && item.getCategory() == AuditMasterDataCategory.BUSINESS_SEGMENT)
                .orElseThrow(() -> new BusinessException("BUSINESS_SEGMENT_NOT_FOUND", "Khong tim thay linh vuc/mang nghiep vu"));
    }

    private AuditAppendix getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_APPENDIX_NOT_FOUND", "Khong tim thay phu luc", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> businessSegmentsById(UUID tenantId) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("businessSegmentCode", "Mã mảng nghiệp vụ"),
                new ExportColumn("sampleType", "Loại mẫu chọn"),
                new ExportColumn("appendixCode", "Mã phụ lục"),
                new ExportColumn("note", "Ghi chú"),
                new ExportColumn("active", "Đang áp dụng"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> businessSegments = businessSegmentsById(tenantId);
        return repository.findByTenantIdOrderByAppendixCodeAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("businessSegmentCode", codeOf(businessSegments.get(item.getBusinessSegmentId())));
                    row.put("sampleType", item.getSampleType());
                    row.put("appendixCode", item.getAppendixCode());
                    row.put("note", item.getNote());
                    row.put("active", item.isActive() ? "Y" : "N");
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

    private AuditAppendixResponse toResponse(AuditAppendix item, Map<UUID, AuditMasterDataItem> businessSegments) {
        AuditMasterDataItem segment = item.getBusinessSegmentId() == null ? null : businessSegments.get(item.getBusinessSegmentId());
        return new AuditAppendixResponse(item.getId(), item.getBusinessSegmentId(),
                segment == null ? null : segment.getCode(), segment == null ? null : segment.getName(),
                item.getSampleType(), item.getAppendixCode(), item.getNote(), item.isActive());
    }
}
