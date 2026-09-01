package com.govia.audit.processstep.service;

import com.govia.audit.controlpoint.entity.AuditControlPoint;
import com.govia.audit.controlpoint.repository.AuditControlPointRepository;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.processstep.dto.AuditProcessStepDetailRequest;
import com.govia.audit.processstep.dto.AuditProcessStepDetailResponse;
import com.govia.audit.processstep.entity.AuditProcessStepDetail;
import com.govia.audit.processstep.entity.AuditProcessStepSummary;
import com.govia.audit.processstep.repository.AuditProcessStepDetailRepository;
import com.govia.audit.processstep.repository.AuditProcessStepSummaryRepository;
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

/** CRUD + Import/Export cho danh muc "Buoc quy trinh chi tiet" (sheet ZTC_BQT_MAP). */
@Service
public class AuditProcessStepDetailService {

    private final AuditProcessStepDetailRepository repository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditProcessStepSummaryRepository processStepSummaryRepository;
    private final AuditControlPointRepository controlPointRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditProcessStepDetailService(AuditProcessStepDetailRepository repository, AuditMasterDataItemRepository masterDataItemRepository,
                                          AuditProcessStepSummaryRepository processStepSummaryRepository,
                                          AuditControlPointRepository controlPointRepository, AuditLogService auditLogService,
                                          ExcelExportService excelExportService, WordExportService wordExportService,
                                          ExcelImportService excelImportService) {
        this.repository = repository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.processStepSummaryRepository = processStepSummaryRepository;
        this.controlPointRepository = controlPointRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditProcessStepDetailResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        Map<UUID, AuditProcessStepSummary> summaries = summariesById(tenantId);
        Map<UUID, AuditControlPoint> controlPoints = controlPointsById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> toResponse(item, segments, summaries, controlPoints)).toList();
    }

    @Transactional
    public AuditProcessStepDetailResponse create(AuditProcessStepDetailRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);
        validateBusinessSegment(tenantId, request.businessSegmentId());
        validateProcessStepSummary(tenantId, request.processStepSummaryId());
        validateControlPoint(tenantId, request.controlPointId());

        AuditProcessStepDetail item = new AuditProcessStepDetail();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditProcessStepDetail", item.getId(), AuditAction.CREATE, "Tao buoc quy trinh chi tiet: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId), summariesById(tenantId), controlPointsById(tenantId));
    }

    @Transactional
    public AuditProcessStepDetailResponse update(UUID id, AuditProcessStepDetailRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditProcessStepDetail item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);
        validateBusinessSegment(tenantId, request.businessSegmentId());
        validateProcessStepSummary(tenantId, request.processStepSummaryId());
        validateControlPoint(tenantId, request.controlPointId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditProcessStepDetail", item.getId(), AuditAction.UPDATE, "Cap nhat buoc quy trinh chi tiet: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId), summariesById(tenantId), controlPointsById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditProcessStepDetail item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditProcessStepDetail", id, AuditAction.DELETE, "Xoa buoc quy trinh chi tiet: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_process_step_detail", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Bước quy trình chi tiết", exportColumns(), exportRows());
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
        Map<String, UUID> summaryIdsByCode = new HashMap<>();
        processStepSummaryRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(s -> summaryIdsByCode.put(s.getCode(), s.getId()));
        Map<String, UUID> controlPointIdsByCode = new HashMap<>();
        controlPointRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> controlPointIdsByCode.put(c.getCode(), c.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String code = row.get("code");
                if (isBlank(code)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma BQT chi tiet");
                }
                String segmentCode = row.get("businessSegmentCode");
                UUID businessSegmentId = isBlank(segmentCode) ? null : segmentIdsByCode.get(segmentCode.trim());
                String summaryCode = row.get("processStepSummaryCode");
                UUID processStepSummaryId = isBlank(summaryCode) ? null : summaryIdsByCode.get(summaryCode.trim());
                String controlPointCode = row.get("controlPointCode");
                UUID controlPointId = isBlank(controlPointCode) ? null : controlPointIdsByCode.get(controlPointCode.trim());

                Optional<AuditProcessStepDetail> existing = repository.findByTenantIdAndCode(tenantId, code.trim());
                AuditProcessStepDetailRequest request = new AuditProcessStepDetailRequest(businessSegmentId, processStepSummaryId,
                        controlPointId, code.trim(), existing.map(AuditProcessStepDetail::isActive).orElse(true));
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

        auditLogService.record("AuditProcessStepDetail", null, AuditAction.CREATE,
                "Import Excel buoc quy trinh chi tiet: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditProcessStepDetail item, AuditProcessStepDetailRequest request) {
        item.setBusinessSegmentId(request.businessSegmentId());
        item.setProcessStepSummaryId(request.processStepSummaryId());
        item.setControlPointId(request.controlPointId());
        item.setCode(request.code());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_PROCESS_STEP_DETAIL_CODE_DUPLICATE", "Ma buoc quy trinh chi tiet da ton tai: " + code);
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

    private void validateProcessStepSummary(UUID tenantId, UUID processStepSummaryId) {
        if (processStepSummaryId == null) {
            return;
        }
        processStepSummaryRepository.findById(processStepSummaryId)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_PROCESS_STEP_SUMMARY_NOT_FOUND", "Khong tim thay buoc quy trinh tong hop"));
    }

    private void validateControlPoint(UUID tenantId, UUID controlPointId) {
        if (controlPointId == null) {
            return;
        }
        controlPointRepository.findById(controlPointId)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CONTROL_POINT_NOT_FOUND", "Khong tim thay chot kiem soat"));
    }

    private AuditProcessStepDetail getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_PROCESS_STEP_DETAIL_NOT_FOUND", "Khong tim thay buoc quy trinh chi tiet", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> businessSegmentsById(UUID tenantId) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private Map<UUID, AuditProcessStepSummary> summariesById(UUID tenantId) {
        return processStepSummaryRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditProcessStepSummary::getId, i -> i));
    }

    private Map<UUID, AuditControlPoint> controlPointsById(UUID tenantId) {
        return controlPointRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditControlPoint::getId, i -> i));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("businessSegmentCode", "Mảng nghiệp vụ"),
                new ExportColumn("processStepSummaryCode", "Mã bước quy trình tổng hợp"),
                new ExportColumn("controlPointCode", "Mã chốt kiểm soát"),
                new ExportColumn("code", "Mã BQT chi tiết"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        Map<UUID, AuditProcessStepSummary> summaries = summariesById(tenantId);
        Map<UUID, AuditControlPoint> controlPoints = controlPointsById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("businessSegmentCode", codeOf(segments.get(item.getBusinessSegmentId())));
                    AuditProcessStepSummary summary = summaries.get(item.getProcessStepSummaryId());
                    row.put("processStepSummaryCode", summary == null ? null : summary.getCode());
                    AuditControlPoint controlPoint = controlPoints.get(item.getControlPointId());
                    row.put("controlPointCode", controlPoint == null ? null : controlPoint.getCode());
                    row.put("code", item.getCode());
                    return row;
                }).toList();
    }

    private String codeOf(AuditMasterDataItem item) {
        return item == null ? null : item.getCode();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private AuditProcessStepDetailResponse toResponse(AuditProcessStepDetail item, Map<UUID, AuditMasterDataItem> segments,
                                                        Map<UUID, AuditProcessStepSummary> summaries, Map<UUID, AuditControlPoint> controlPoints) {
        AuditMasterDataItem segment = item.getBusinessSegmentId() == null ? null : segments.get(item.getBusinessSegmentId());
        AuditProcessStepSummary summary = item.getProcessStepSummaryId() == null ? null : summaries.get(item.getProcessStepSummaryId());
        AuditControlPoint controlPoint = item.getControlPointId() == null ? null : controlPoints.get(item.getControlPointId());
        return new AuditProcessStepDetailResponse(item.getId(), item.getBusinessSegmentId(),
                segment == null ? null : segment.getCode(), segment == null ? null : segment.getName(),
                item.getProcessStepSummaryId(), summary == null ? null : summary.getCode(), summary == null ? null : summary.getName(),
                item.getControlPointId(), controlPoint == null ? null : controlPoint.getCode(), controlPoint == null ? null : controlPoint.getName(),
                item.getCode(), item.isActive());
    }
}
