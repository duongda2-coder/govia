package com.govia.audit.processstepqt.service;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.processstepqt.dto.AuditProcessStepSummaryQtRequest;
import com.govia.audit.processstepqt.dto.AuditProcessStepSummaryQtResponse;
import com.govia.audit.processstepqt.entity.AuditProcessStepSummaryQt;
import com.govia.audit.processstepqt.repository.AuditProcessStepSummaryQtRepository;
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

/** CRUD + Import/Export cho danh muc "Buoc quy trinh tong hop" (sheet ZTC_BQT_TH_QT). */
@Service
public class AuditProcessStepSummaryQtService {

    private final AuditProcessStepSummaryQtRepository repository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditWorkItemQtRepository workItemRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditProcessStepSummaryQtService(AuditProcessStepSummaryQtRepository repository, AuditMasterDataItemRepository masterDataItemRepository,
                                             AuditWorkItemQtRepository workItemRepository, AuditLogService auditLogService,
                                             ExcelExportService excelExportService, WordExportService wordExportService,
                                             ExcelImportService excelImportService) {
        this.repository = repository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.workItemRepository = workItemRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditProcessStepSummaryQtResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        Map<UUID, AuditWorkItemQt> workItems = workItemsById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream().map(item -> toResponse(item, segments, workItems)).toList();
    }

    @Transactional
    public AuditProcessStepSummaryQtResponse create(AuditProcessStepSummaryQtRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), request.applicableYear(), null);
        validateBusinessSegment(tenantId, request.businessSegmentId());
        validateWorkItem(tenantId, request.workItemId());

        AuditProcessStepSummaryQt item = new AuditProcessStepSummaryQt();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditProcessStepSummaryQt", item.getId(), AuditAction.CREATE, "Tao buoc quy trinh tong hop: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId), workItemsById(tenantId));
    }

    @Transactional
    public AuditProcessStepSummaryQtResponse update(UUID id, AuditProcessStepSummaryQtRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditProcessStepSummaryQt item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), request.applicableYear(), id);
        validateBusinessSegment(tenantId, request.businessSegmentId());
        validateWorkItem(tenantId, request.workItemId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditProcessStepSummaryQt", item.getId(), AuditAction.UPDATE, "Cap nhat buoc quy trinh tong hop: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId), workItemsById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditProcessStepSummaryQt item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditProcessStepSummaryQt", id, AuditAction.DELETE, "Xoa buoc quy trinh tong hop: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_process_step_summary_qt", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Bước quy trình tổng hợp", exportColumns(), exportRows());
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
        Map<String, UUID> workItemIdsByCode = new HashMap<>();
        workItemRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(w -> workItemIdsByCode.put(w.getCode(), w.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String code = row.get("code");
                String name = row.get("name");
                if (isBlank(code) || isBlank(name)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma buoc quy trinh tong hop hoac Ten");
                }
                String segmentCode = row.get("businessSegmentCode");
                UUID businessSegmentId = isBlank(segmentCode) ? null : segmentIdsByCode.get(segmentCode.trim());
                String workItemCode = row.get("workItemCode");
                UUID workItemId = isBlank(workItemCode) ? null : workItemIdsByCode.get(workItemCode.trim());
                Integer applicableYear = parseInt(row.get("applicableYear"));

                Optional<AuditProcessStepSummaryQt> existing = repository.findByTenantIdAndCodeAndApplicableYear(tenantId, code.trim(), applicableYear);
                AuditProcessStepSummaryQtRequest request = new AuditProcessStepSummaryQtRequest(businessSegmentId, code.trim(), name.trim(),
                        workItemId, applicableYear, existing.map(AuditProcessStepSummaryQt::isActive).orElse(true));
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

        auditLogService.record("AuditProcessStepSummaryQt", null, AuditAction.CREATE,
                "Import Excel buoc quy trinh tong hop: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditProcessStepSummaryQt item, AuditProcessStepSummaryQtRequest request) {
        item.setBusinessSegmentId(request.businessSegmentId());
        item.setCode(request.code());
        item.setName(request.name());
        item.setWorkItemId(request.workItemId());
        item.setApplicableYear(request.applicableYear());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, Integer applicableYear, UUID excludingId) {
        repository.findByTenantIdAndCodeAndApplicableYear(tenantId, code, applicableYear)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_PROCESS_STEP_SUMMARY_QT_CODE_DUPLICATE",
                            "Ma buoc quy trinh tong hop da ton tai cho nam " + applicableYear + ": " + code);
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

    private void validateWorkItem(UUID tenantId, UUID workItemId) {
        if (workItemId == null) {
            return;
        }
        workItemRepository.findById(workItemId)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_WORK_ITEM_QT_NOT_FOUND", "Khong tim thay cong viec quy trinh"));
    }

    private AuditProcessStepSummaryQt getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_PROCESS_STEP_SUMMARY_QT_NOT_FOUND", "Khong tim thay buoc quy trinh tong hop", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> businessSegmentsById(UUID tenantId) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private Map<UUID, AuditWorkItemQt> workItemsById(UUID tenantId) {
        return workItemRepository.findByTenantIdOrderByCodeAsc(tenantId).stream().collect(Collectors.toMap(AuditWorkItemQt::getId, i -> i));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("businessSegmentCode", "Mảng nghiệp vụ"),
                new ExportColumn("code", "Mã bước quy trình tổng hợp"),
                new ExportColumn("name", "Tên bước quy trình tổng hợp"),
                new ExportColumn("workItemCode", "Mã công việc"),
                new ExportColumn("applicableYear", "Năm"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> segments = businessSegmentsById(tenantId);
        Map<UUID, AuditWorkItemQt> workItems = workItemsById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("businessSegmentCode", codeOf(segments.get(item.getBusinessSegmentId())));
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    AuditWorkItemQt workItem = workItems.get(item.getWorkItemId());
                    row.put("workItemCode", workItem == null ? null : workItem.getCode());
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

    private AuditProcessStepSummaryQtResponse toResponse(AuditProcessStepSummaryQt item, Map<UUID, AuditMasterDataItem> segments,
                                                           Map<UUID, AuditWorkItemQt> workItems) {
        AuditMasterDataItem segment = item.getBusinessSegmentId() == null ? null : segments.get(item.getBusinessSegmentId());
        AuditWorkItemQt workItem = item.getWorkItemId() == null ? null : workItems.get(item.getWorkItemId());
        return new AuditProcessStepSummaryQtResponse(item.getId(), item.getBusinessSegmentId(),
                segment == null ? null : segment.getCode(), segment == null ? null : segment.getName(),
                item.getCode(), item.getName(), item.getWorkItemId(),
                workItem == null ? null : workItem.getCode(), workItem == null ? null : workItem.getName(),
                item.getApplicableYear(), item.isActive());
    }
}
