package com.govia.audit.khkt.scale.service;

import com.govia.audit.khkt.scale.dto.AuditKhktScaleRequest;
import com.govia.audit.khkt.scale.dto.AuditKhktScaleResponse;
import com.govia.audit.khkt.scale.entity.AuditKhktScale;
import com.govia.audit.khkt.scale.repository.AuditKhktScaleRepository;
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

/** CRUD + Import/Export cho danh muc "Quy mo tin dung va huy dong von cua chi nhanh" (sheet ZTC_KHKT_QM). */
@Service
public class AuditKhktScaleService {

    private final AuditKhktScaleRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditKhktScaleService(AuditKhktScaleRepository repository, AuditLogService auditLogService,
                                  ExcelExportService excelExportService, WordExportService wordExportService,
                                  ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditKhktScaleResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderBySortOrderAsc(tenantId).stream().map(this::toResponse).toList();
    }

    /** Tra "Quy mo tin dung" cho 1 gia tri Du no noi bang, theo logic sheet ZTC_KHKT_THANG: lay STT
     * cua muc co credit_threshold lon nhat ma khong vuot qua gia tri (vi du DUNO >= 10.000 (muc 3)
     * -> quy mo 3; >= 5.000 (muc 2) -> quy mo 2; con lai -> muc thap nhat). */
    @Transactional(readOnly = true)
    public Integer resolveCreditScale(BigDecimal onBalanceSheetLoan) {
        return resolveScale(onBalanceSheetLoan, AuditKhktScale::getCreditThreshold);
    }

    /** Tra "Quy mo huy dong von" cho 1 gia tri Nguon von - cung logic voi resolveCreditScale nhung
     * so sanh voi funding_threshold. */
    @Transactional(readOnly = true)
    public Integer resolveFundingScale(BigDecimal fundingSource) {
        return resolveScale(fundingSource, AuditKhktScale::getFundingThreshold);
    }

    private Integer resolveScale(BigDecimal value, java.util.function.Function<AuditKhktScale, BigDecimal> thresholdFn) {
        if (value == null) {
            return null;
        }
        UUID tenantId = TenantContext.getTenantId();
        List<AuditKhktScale> scales = repository.findByTenantIdOrderBySortOrderAsc(tenantId).stream()
                .filter(AuditKhktScale::isActive).toList();
        if (scales.isEmpty()) {
            return null;
        }
        Integer result = scales.get(0).getSortOrder();
        for (AuditKhktScale scale : scales) {
            BigDecimal threshold = thresholdFn.apply(scale);
            if (threshold != null && value.compareTo(threshold) >= 0) {
                result = scale.getSortOrder();
            }
        }
        return result;
    }

    @Transactional
    public AuditKhktScaleResponse create(AuditKhktScaleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicateSortOrder(tenantId, request.sortOrder(), null);

        AuditKhktScale item = new AuditKhktScale();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditKhktScale", item.getId(), AuditAction.CREATE, "Tao quy mo tin dung/huy dong von: " + item.getScaleName());
        return toResponse(item);
    }

    @Transactional
    public AuditKhktScaleResponse update(UUID id, AuditKhktScaleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditKhktScale item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicateSortOrder(tenantId, request.sortOrder(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditKhktScale", item.getId(), AuditAction.UPDATE, "Cap nhat quy mo tin dung/huy dong von: " + item.getScaleName());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditKhktScale item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditKhktScale", id, AuditAction.DELETE, "Xoa quy mo tin dung/huy dong von: " + item.getScaleName());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_khkt_scale", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Quy mô tín dụng và huy động vốn của chi nhánh", exportColumns(), exportRows());
    }

    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), exportColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                Integer sortOrder = parseInt(row.get("sortOrder"));
                String scaleName = row.get("scaleName");
                if (sortOrder == null || isBlank(scaleName)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu STT hoac Ten quy mo");
                }
                create(new AuditKhktScaleRequest(sortOrder, parseDecimal(row.get("creditThreshold")),
                        parseDecimal(row.get("fundingThreshold")), scaleName.trim(), true));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("AuditKhktScale", null, AuditAction.CREATE,
                "Import Excel quy mo tin dung/huy dong von: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditKhktScale item, AuditKhktScaleRequest request) {
        item.setSortOrder(request.sortOrder());
        item.setCreditThreshold(request.creditThreshold());
        item.setFundingThreshold(request.fundingThreshold());
        item.setScaleName(request.scaleName());
        item.setActive(request.active());
    }

    private void checkNoDuplicateSortOrder(UUID tenantId, Integer sortOrder, UUID excludingId) {
        repository.findByTenantIdAndSortOrder(tenantId, sortOrder)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_KHKT_SCALE_SORT_ORDER_DUPLICATE", "STT da ton tai: " + sortOrder);
                });
    }

    private AuditKhktScale getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_KHKT_SCALE_NOT_FOUND", "Khong tim thay quy mo tin dung/huy dong von", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("sortOrder", "STT"),
                new ExportColumn("creditThreshold", "Tín dụng"),
                new ExportColumn("fundingThreshold", "Huy động vốn"),
                new ExportColumn("scaleName", "Tên quy mô"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderBySortOrderAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("sortOrder", item.getSortOrder());
                    row.put("creditThreshold", item.getCreditThreshold());
                    row.put("fundingThreshold", item.getFundingThreshold());
                    row.put("scaleName", item.getScaleName());
                    return row;
                }).toList();
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

    private AuditKhktScaleResponse toResponse(AuditKhktScale item) {
        return new AuditKhktScaleResponse(item.getId(), item.getSortOrder(), item.getCreditThreshold(),
                item.getFundingThreshold(), item.getScaleName(), item.isActive());
    }
}
