package com.govia.audit.controlpoint.service;

import com.govia.audit.controlpoint.dto.AuditControlPointRequest;
import com.govia.audit.controlpoint.dto.AuditControlPointResponse;
import com.govia.audit.controlpoint.entity.AuditControlPoint;
import com.govia.audit.controlpoint.entity.AuditControlType;
import com.govia.audit.controlpoint.repository.AuditControlPointRepository;
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
import java.util.UUID;
import java.util.stream.Collectors;

/** CRUD + Import/Export cho danh muc "Chot kiem soat" (sheet ZTC_CKS). */
@Service
public class AuditControlPointService {

    private final AuditControlPointRepository repository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditControlPointService(AuditControlPointRepository repository, AuditMasterDataItemRepository masterDataItemRepository,
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
    public List<AuditControlPointResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> businessSegments = businessSegmentsById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream().map(item -> toResponse(item, businessSegments)).toList();
    }

    @Transactional
    public AuditControlPointResponse create(AuditControlPointRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateCode(tenantId, request.code(), null);
        validateBusinessSegment(tenantId, request.businessSegmentId());

        AuditControlPoint item = new AuditControlPoint();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditControlPoint", item.getId(), AuditAction.CREATE, "Tao chot kiem soat: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId));
    }

    @Transactional
    public AuditControlPointResponse update(UUID id, AuditControlPointRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditControlPoint item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateCode(tenantId, request.code(), id);
        validateBusinessSegment(tenantId, request.businessSegmentId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditControlPoint", item.getId(), AuditAction.UPDATE, "Cap nhat chot kiem soat: " + item.getCode());
        return toResponse(item, businessSegmentsById(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditControlPoint item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditControlPoint", id, AuditAction.DELETE, "Xoa chot kiem soat: " + item.getCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_control_point", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Chốt kiểm soát", exportColumns(), exportRows());
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
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma CKS hoac Ten CKS");
                }
                String segmentCode = row.get("businessSegmentCode");
                UUID businessSegmentId = isBlank(segmentCode) ? null : segmentIdsByCode.get(segmentCode.trim());
                create(new AuditControlPointRequest(businessSegmentId, code.trim(), name.trim(),
                        emptyToNull(row.get("possibleRisk")), emptyToNull(row.get("controlPointByStep")),
                        emptyToNull(row.get("actualControl")), parseEnum(AuditControlType.class, row.get("controlType")),
                        parseEnum(AuditLevel.class, row.get("controlFrequency")), emptyToNull(row.get("auditProcedure")),
                        emptyToNull(row.get("residualRiskAssessment")), emptyToNull(row.get("processRegulation")),
                        emptyToNull(row.get("referenceClause")), emptyToNull(row.get("processEffectiveness")),
                        emptyToNull(row.get("controlEffectivenessAssessment")), emptyToNull(row.get("controlEfficiencyAssessment")), true));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditControlPoint", null, AuditAction.CREATE,
                "Import Excel chot kiem soat: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditControlPoint item, AuditControlPointRequest request) {
        item.setBusinessSegmentId(request.businessSegmentId());
        item.setCode(request.code());
        item.setName(request.name());
        item.setPossibleRisk(request.possibleRisk());
        item.setControlPointByStep(request.controlPointByStep());
        item.setActualControl(request.actualControl());
        item.setControlType(request.controlType());
        item.setControlFrequency(request.controlFrequency());
        item.setAuditProcedure(request.auditProcedure());
        item.setResidualRiskAssessment(request.residualRiskAssessment());
        item.setProcessRegulation(request.processRegulation());
        item.setReferenceClause(request.referenceClause());
        item.setProcessEffectiveness(request.processEffectiveness());
        item.setControlEffectivenessAssessment(request.controlEffectivenessAssessment());
        item.setControlEfficiencyAssessment(request.controlEfficiencyAssessment());
        item.setActive(request.active());
    }

    private void checkNoDuplicateCode(UUID tenantId, String code, UUID excludingId) {
        repository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CONTROL_POINT_CODE_DUPLICATE", "Ma chot kiem soat da ton tai: " + code);
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

    private AuditControlPoint getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CONTROL_POINT_NOT_FOUND", "Khong tim thay chot kiem soat", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, AuditMasterDataItem> businessSegmentsById(UUID tenantId) {
        return masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.BUSINESS_SEGMENT)
                .stream().collect(Collectors.toMap(AuditMasterDataItem::getId, i -> i));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("businessSegmentCode", "Mảng nghiệp vụ"),
                new ExportColumn("code", "Mã CKS"),
                new ExportColumn("name", "Tên CKS"),
                new ExportColumn("possibleRisk", "Rủi ro có thể xảy ra"),
                new ExportColumn("controlPointByStep", "Chốt kiểm soát theo bước quy trình"),
                new ExportColumn("actualControl", "Kiểm soát thực tế tại đơn vị"),
                new ExportColumn("controlType", "Loại hình KS"),
                new ExportColumn("controlFrequency", "Tần suất kiểm soát"),
                new ExportColumn("auditProcedure", "Thủ tục kiểm toán"),
                new ExportColumn("residualRiskAssessment", "Đánh giá rủi ro còn lại"),
                new ExportColumn("processRegulation", "Quy trình, Quy định sử dụng"),
                new ExportColumn("referenceClause", "Điều khoản tham chiếu"),
                new ExportColumn("processEffectiveness", "Hiệu lực quy trình"),
                new ExportColumn("controlEffectivenessAssessment", "Đánh giá hiệu lực chốt KS"),
                new ExportColumn("controlEfficiencyAssessment", "Đánh giá hiệu quả chốt KS"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, AuditMasterDataItem> businessSegments = businessSegmentsById(tenantId);
        return repository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("businessSegmentCode", codeOf(businessSegments.get(item.getBusinessSegmentId())));
                    row.put("code", item.getCode());
                    row.put("name", item.getName());
                    row.put("possibleRisk", item.getPossibleRisk());
                    row.put("controlPointByStep", item.getControlPointByStep());
                    row.put("actualControl", item.getActualControl());
                    row.put("controlType", item.getControlType());
                    row.put("controlFrequency", item.getControlFrequency());
                    row.put("auditProcedure", item.getAuditProcedure());
                    row.put("residualRiskAssessment", item.getResidualRiskAssessment());
                    row.put("processRegulation", item.getProcessRegulation());
                    row.put("referenceClause", item.getReferenceClause());
                    row.put("processEffectiveness", item.getProcessEffectiveness());
                    row.put("controlEffectivenessAssessment", item.getControlEffectivenessAssessment());
                    row.put("controlEfficiencyAssessment", item.getControlEfficiencyAssessment());
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

    private AuditControlPointResponse toResponse(AuditControlPoint item, Map<UUID, AuditMasterDataItem> businessSegments) {
        AuditMasterDataItem segment = item.getBusinessSegmentId() == null ? null : businessSegments.get(item.getBusinessSegmentId());
        return new AuditControlPointResponse(item.getId(), item.getBusinessSegmentId(),
                segment == null ? null : segment.getCode(), segment == null ? null : segment.getName(),
                item.getCode(), item.getName(), item.getPossibleRisk(), item.getControlPointByStep(), item.getActualControl(),
                item.getControlType(), item.getControlFrequency(), item.getAuditProcedure(), item.getResidualRiskAssessment(),
                item.getProcessRegulation(), item.getReferenceClause(), item.getProcessEffectiveness(),
                item.getControlEffectivenessAssessment(), item.getControlEfficiencyAssessment(), item.isActive());
    }
}
