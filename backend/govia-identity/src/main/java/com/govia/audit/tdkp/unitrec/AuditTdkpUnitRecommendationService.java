package com.govia.audit.tdkp.unitrec;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.tdkp.common.TdkpMasterData;
import com.govia.audit.tdkp.common.TdkpStatus;
import com.govia.audit.tdkp.common.TdkpSupport;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Sheet 6 - ZTC_TDKP_KTNB: theo dõi kiến nghị của Đơn vị đối với KTNB (nhập tay, import Excel, extract Excel). */
@Service
public class AuditTdkpUnitRecommendationService {

    private final AuditTdkpUnitRecommendationRepository repository;
    private final TdkpMasterData masterData;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    public AuditTdkpUnitRecommendationService(AuditTdkpUnitRecommendationRepository repository, TdkpMasterData masterData,
                                              AuditLogService auditLogService, ExcelExportService excelExportService,
                                              ExcelImportService excelImportService) {
        this.repository = repository;
        this.masterData = masterData;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditTdkpUnitRecommendationDto.Response> list() {
        Map<UUID, AuditObjectUnit> units = masterData.units();
        return repository.findByTenantIdOrderByReportDateDescCodeDesc(TenantContext.getTenantId()).stream().map(i -> toResponse(i, units)).toList();
    }

    @Transactional
    public AuditTdkpUnitRecommendationDto.Response create(AuditTdkpUnitRecommendationDto.Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditTdkpUnitRecommendation item = new AuditTdkpUnitRecommendation();
        item.setTenantId(tenantId);
        item.setCode(TdkpSupport.nextCode("KNIA", repository.findByTenantIdOrderByReportDateDescCodeDesc(tenantId).stream()
                .map(AuditTdkpUnitRecommendation::getCode).toList()));
        apply(item, request);
        item = repository.save(item);
        auditLogService.record("AuditTdkpUnitRecommendation", item.getId(), AuditAction.CREATE, "Tao kien nghi don vi doi voi KTNB: " + item.getCode());
        return toResponse(item, masterData.units());
    }

    @Transactional
    public AuditTdkpUnitRecommendationDto.Response update(UUID id, AuditTdkpUnitRecommendationDto.Request request) {
        AuditTdkpUnitRecommendation item = getOwnedOrThrow(id);
        apply(item, request);
        item = repository.save(item);
        auditLogService.record("AuditTdkpUnitRecommendation", item.getId(), AuditAction.UPDATE, "Cap nhat kien nghi don vi doi voi KTNB: " + item.getCode());
        return toResponse(item, masterData.units());
    }

    @Transactional
    public void delete(UUID id) {
        AuditTdkpUnitRecommendation item = getOwnedOrThrow(id);
        repository.delete(item);
        auditLogService.record("AuditTdkpUnitRecommendation", id, AuditAction.DELETE, "Xoa kien nghi don vi doi voi KTNB: " + item.getCode());
    }

    private List<ExportColumn> columns() {
        return List.of(new ExportColumn("code", "Mã Kiến nghị KTNB"), new ExportColumn("reportNumber", "Số báo cáo"), new ExportColumn("reportDate", "Ngày báo cáo"),
                new ExportColumn("unit", "Đơn vị kiến nghị"), new ExportColumn("recommendationTarget", "Đối tượng kiến nghị"),
                new ExportColumn("content", "Nội dung kiến nghị"), new ExportColumn("deadline", "Thời hạn hoàn thành"),
                new ExportColumn("implementation", "Tình hình thực hiện kiến nghị"), new ExportColumn("status", "Hiện trạng"),
                new ExportColumn("evaluation", "Đánh giá tình hình thực hiện kiến nghị"), new ExportColumn("lastEditedDate", "Thời gian chỉnh sửa"),
                new ExportColumn("deadlineState", "Trạng thái"), new ExportColumn("note", "Ghi Chú"),
                new ExportColumn("approvalStatus", "Trạng thái phê duyệt"));
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        List<Map<String, Object>> rows = list().stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("code", r.code());
            row.put("reportNumber", r.reportNumber());
            row.put("reportDate", r.reportDate());
            row.put("unit", r.unitName());
            row.put("recommendationTarget", r.recommendationTarget());
            row.put("content", r.content());
            row.put("deadline", r.deadline());
            row.put("implementation", r.implementation());
            row.put("status", r.statusLabel());
            row.put("evaluation", r.evaluation());
            row.put("lastEditedDate", r.lastEditedDate());
            row.put("deadlineState", r.deadlineStateLabel());
            row.put("note", r.note());
            row.put("approvalStatus", r.approvalStatusLabel());
            return row;
        }).toList();
        return excelExportService.export("TDKP_KTNB", columns(), rows);
    }

    /** Import: "Đơn vị kiến nghị" phải có trong danh mục đối tượng kiểm toán, "Hiện trạng" phải là Đã/Đang/Chưa thực hiện; có Mã đã tồn tại thì cập nhật. */
    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), columns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }
        UUID tenantId = TenantContext.getTenantId();
        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                if (TdkpSupport.isBlank(row.get("content"))) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Noi dung kien nghi");
                }
                AuditTdkpUnitRecommendationDto.Request request = new AuditTdkpUnitRecommendationDto.Request(TdkpSupport.emptyToNull(row.get("reportNumber")),
                        TdkpSupport.parseDate(row.get("reportDate")), masterData.resolveUnit(row.get("unit"), "Don vi kien nghi"),
                        TdkpSupport.emptyToNull(row.get("recommendationTarget")), row.get("content").trim(),
                        TdkpSupport.parseDate(row.get("deadline")), TdkpSupport.emptyToNull(row.get("implementation")), TdkpStatus.parse(row.get("status")),
                        TdkpSupport.emptyToNull(row.get("evaluation")), TdkpSupport.emptyToNull(row.get("note")),
                        TdkpSupport.parseApprovalStatus(row.get("approvalStatus")));
                String code = TdkpSupport.emptyToNull(row.get("code"));
                AuditTdkpUnitRecommendation existing = code == null ? null : repository.findByTenantIdAndCode(tenantId, code).orElse(null);
                if (existing != null) {
                    update(existing.getId(), request);
                } else {
                    create(request);
                }
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(i + 2, e.getMessage()));
            }
        }
        auditLogService.record("AuditTdkpUnitRecommendation", null, AuditAction.CREATE,
                "Import Excel kien nghi don vi doi voi KTNB: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void apply(AuditTdkpUnitRecommendation item, AuditTdkpUnitRecommendationDto.Request request) {
        item.setReportNumber(TdkpSupport.emptyToNull(request.reportNumber()));
        item.setReportDate(request.reportDate());
        item.setUnitId(masterData.requireUnit(request.unitId(), "Don vi kien nghi"));
        item.setRecommendationTarget(TdkpSupport.emptyToNull(request.recommendationTarget()));
        item.setContent(request.content().trim());
        item.setDeadline(request.deadline());
        item.setImplementation(TdkpSupport.emptyToNull(request.implementation()));
        item.setStatus(request.status());
        item.setEvaluation(TdkpSupport.emptyToNull(request.evaluation()));
        item.setNote(TdkpSupport.emptyToNull(request.note()));
        item.setApprovalStatus(request.approvalStatus());
    }

    private AuditTdkpUnitRecommendation getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findById(id).filter(i -> i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("TDKP_UNIT_RECOMMENDATION_NOT_FOUND", "Khong tim thay kien nghi", HttpStatus.NOT_FOUND));
    }

    private AuditTdkpUnitRecommendationDto.Response toResponse(AuditTdkpUnitRecommendation item, Map<UUID, AuditObjectUnit> units) {
        AuditObjectUnit unit = item.getUnitId() == null ? null : units.get(item.getUnitId());
        String state = TdkpSupport.deadlineState(item.getDeadline());
        Instant editedAt = item.getUpdatedAt() != null ? item.getUpdatedAt() : item.getCreatedAt();
        LocalDate lastEditedDate = editedAt == null ? null : editedAt.atZone(ZoneId.systemDefault()).toLocalDate();
        return new AuditTdkpUnitRecommendationDto.Response(item.getId(), item.getCode(), item.getReportNumber(), item.getReportDate(), item.getUnitId(),
                unit == null ? null : unit.getCode(), unit == null ? null : unit.getName(), item.getRecommendationTarget(), item.getContent(), item.getDeadline(),
                item.getImplementation(), item.getStatus(), TdkpStatus.labelOf(item.getStatus()), item.getEvaluation(), lastEditedDate, state,
                TdkpSupport.deadlineLabel(state), item.getNote(), item.getApprovalStatus(), TdkpSupport.approvalStatusLabel(item.getApprovalStatus()));
    }
}
