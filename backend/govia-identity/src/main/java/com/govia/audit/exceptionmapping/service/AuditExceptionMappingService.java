package com.govia.audit.exceptionmapping.service;

import com.govia.audit.exceptionmapping.dto.AuditExceptionMappingRequest;
import com.govia.audit.exceptionmapping.dto.AuditExceptionMappingResponse;
import com.govia.audit.exceptionmapping.entity.AuditExceptionMapping;
import com.govia.audit.exceptionmapping.repository.AuditExceptionMappingRepository;
import com.govia.audit.exceptiontype.entity.AuditExceptionType;
import com.govia.audit.exceptiontype.repository.AuditExceptionTypeRepository;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.processstep.entity.AuditProcessStepDetail;
import com.govia.audit.processstep.repository.AuditProcessStepDetailRepository;
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

/** CRUD + Import/Export cho danh muc "Mapping ton tai sai sot" (sheet ZTC_TTSS_MAP). */
@Service
public class AuditExceptionMappingService {

    private final AuditExceptionMappingRepository repository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditProcessStepDetailRepository processStepDetailRepository;
    private final AuditExceptionTypeRepository exceptionTypeRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditExceptionMappingService(AuditExceptionMappingRepository repository, AuditMasterDataItemRepository masterDataItemRepository,
                                         AuditProcessStepDetailRepository processStepDetailRepository,
                                         AuditExceptionTypeRepository exceptionTypeRepository, AuditLogService auditLogService,
                                         ExcelExportService excelExportService, WordExportService wordExportService,
                                         ExcelImportService excelImportService) {
        this.repository = repository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.processStepDetailRepository = processStepDetailRepository;
        this.exceptionTypeRepository = exceptionTypeRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditExceptionMappingResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        Map<UUID, AuditProcessStepDetail> details = processStepDetailsById(tenantId);
        Map<UUID, AuditExceptionType> exceptionTypes = exceptionTypesById(tenantId);
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(item -> toResponse(item, segments, details, exceptionTypes)).toList();
    }

    @Transactional
    public AuditExceptionMappingResponse create(AuditExceptionMappingRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.processStepDetailId(), request.exceptionTypeId(), null);
        validateBusinessSegment(tenantId, request.businessSegmentId());
        validateProcessStepDetail(tenantId, request.processStepDetailId());
        validateExceptionType(tenantId, request.exceptionTypeId());

        AuditExceptionMapping item = new AuditExceptionMapping();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditExceptionMapping", item.getId(), AuditAction.CREATE, "Tao mapping ton tai sai sot");
        return toResponse(item, businessSegmentsById(tenantId), processStepDetailsById(tenantId), exceptionTypesById(tenantId));
    }

    @Transactional
    public AuditExceptionMappingResponse update(UUID id, AuditExceptionMappingRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditExceptionMapping item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.processStepDetailId(), request.exceptionTypeId(), id);
        validateBusinessSegment(tenantId, request.businessSegmentId());
        validateProcessStepDetail(tenantId, request.processStepDetailId());
        validateExceptionType(tenantId, request.exceptionTypeId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditExceptionMapping", item.getId(), AuditAction.UPDATE, "Cap nhat mapping ton tai sai sot");
        return toResponse(item, businessSegmentsById(tenantId), processStepDetailsById(tenantId), exceptionTypesById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditExceptionMapping item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditExceptionMapping", id, AuditAction.DELETE, "Xoa mapping ton tai sai sot");
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_exception_mapping", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Mapping tồn tại sai sót", exportColumns(), exportRows());
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
        Map<String, UUID> detailIdsByCode = new HashMap<>();
        processStepDetailRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(d -> detailIdsByCode.put(d.getCode(), d.getId()));
        Map<String, UUID> exceptionTypeIdsByCode = new HashMap<>();
        exceptionTypeRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(e -> exceptionTypeIdsByCode.put(e.getCode(), e.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String detailCode = row.get("processStepDetailCode");
                String exceptionTypeCode = row.get("exceptionTypeCode");
                if (isBlank(detailCode) || isBlank(exceptionTypeCode)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma BQT_CT hoac Ma TTSS");
                }
                UUID processStepDetailId = detailIdsByCode.get(detailCode.trim());
                UUID exceptionTypeId = exceptionTypeIdsByCode.get(exceptionTypeCode.trim());
                if (processStepDetailId == null) {
                    throw new BusinessException("AUDIT_PROCESS_STEP_DETAIL_NOT_FOUND", "Khong tim thay ma BQT_CT: " + detailCode);
                }
                if (exceptionTypeId == null) {
                    throw new BusinessException("AUDIT_EXCEPTION_TYPE_NOT_FOUND", "Khong tim thay ma TTSS: " + exceptionTypeCode);
                }
                String segmentCode = row.get("businessSegmentCode");
                UUID businessSegmentId = isBlank(segmentCode) ? null : segmentIdsByCode.get(segmentCode.trim());

                Optional<AuditExceptionMapping> existing =
                        repository.findByTenantIdAndProcessStepDetailIdAndExceptionTypeId(tenantId, processStepDetailId, exceptionTypeId);
                AuditExceptionMappingRequest request = new AuditExceptionMappingRequest(businessSegmentId, processStepDetailId, exceptionTypeId,
                        existing.map(AuditExceptionMapping::isActive).orElse(true));
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

        auditLogService.record("AuditExceptionMapping", null, AuditAction.CREATE,
                "Import Excel mapping ton tai sai sot: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditExceptionMapping item, AuditExceptionMappingRequest request) {
        item.setBusinessSegmentId(request.businessSegmentId());
        item.setProcessStepDetailId(request.processStepDetailId());
        item.setExceptionTypeId(request.exceptionTypeId());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, UUID processStepDetailId, UUID exceptionTypeId, UUID excludingId) {
        repository.findByTenantIdAndProcessStepDetailIdAndExceptionTypeId(tenantId, processStepDetailId, exceptionTypeId)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_EXCEPTION_MAPPING_DUPLICATE", "Mapping nay da ton tai");
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

    private void validateProcessStepDetail(UUID tenantId, UUID processStepDetailId) {
        processStepDetailRepository.findById(processStepDetailId)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_PROCESS_STEP_DETAIL_NOT_FOUND", "Khong tim thay buoc quy trinh chi tiet"));
    }

    private void validateExceptionType(UUID tenantId, UUID exceptionTypeId) {
        exceptionTypeRepository.findById(exceptionTypeId)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_EXCEPTION_TYPE_NOT_FOUND", "Khong tim thay loai ton tai sai sot"));
    }

    private AuditExceptionMapping getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_EXCEPTION_MAPPING_NOT_FOUND", "Khong tim thay mapping ton tai sai sot", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> businessSegmentsById(UUID tenantId) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private Map<UUID, AuditProcessStepDetail> processStepDetailsById(UUID tenantId) {
        return processStepDetailRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditProcessStepDetail::getId, i -> i));
    }

    private Map<UUID, AuditExceptionType> exceptionTypesById(UUID tenantId) {
        return exceptionTypeRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditExceptionType::getId, i -> i));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("businessSegmentCode", "Mã mảng nv"),
                new ExportColumn("processStepDetailCode", "Mã BQT_CT"),
                new ExportColumn("exceptionTypeCode", "Mã TTSS"),
                new ExportColumn("exceptionTypeName", "Tên TTSS"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        Map<UUID, AuditProcessStepDetail> details = processStepDetailsById(tenantId);
        Map<UUID, AuditExceptionType> exceptionTypes = exceptionTypesById(tenantId);
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("businessSegmentCode", codeOf(segments.get(item.getBusinessSegmentId())));
                    AuditProcessStepDetail detail = details.get(item.getProcessStepDetailId());
                    row.put("processStepDetailCode", detail == null ? null : detail.getCode());
                    AuditExceptionType exceptionType = exceptionTypes.get(item.getExceptionTypeId());
                    row.put("exceptionTypeCode", exceptionType == null ? null : exceptionType.getCode());
                    row.put("exceptionTypeName", exceptionType == null ? null : exceptionType.getName());
                    return row;
                }).toList();
    }

    private String codeOf(AuditMasterDataItem item) {
        return item == null ? null : item.getCode();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private AuditExceptionMappingResponse toResponse(AuditExceptionMapping item, Map<UUID, AuditMasterDataItem> segments,
                                                       Map<UUID, AuditProcessStepDetail> details, Map<UUID, AuditExceptionType> exceptionTypes) {
        AuditMasterDataItem segment = item.getBusinessSegmentId() == null ? null : segments.get(item.getBusinessSegmentId());
        AuditProcessStepDetail detail = details.get(item.getProcessStepDetailId());
        AuditExceptionType exceptionType = exceptionTypes.get(item.getExceptionTypeId());
        return new AuditExceptionMappingResponse(item.getId(), item.getBusinessSegmentId(),
                segment == null ? null : segment.getCode(), segment == null ? null : segment.getName(),
                item.getProcessStepDetailId(), detail == null ? null : detail.getCode(),
                item.getExceptionTypeId(), exceptionType == null ? null : exceptionType.getCode(), exceptionType == null ? null : exceptionType.getName(),
                item.isActive());
    }
}
