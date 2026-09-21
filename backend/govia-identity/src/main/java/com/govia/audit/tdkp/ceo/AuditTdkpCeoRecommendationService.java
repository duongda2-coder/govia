package com.govia.audit.tdkp.ceo;

import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.tdkp.common.TdkpMasterData;
import com.govia.audit.tdkp.common.TdkpStatus;
import com.govia.audit.tdkp.common.TdkpSupport;
import com.govia.audit.tdkp.common.TdkpTarget;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Sheet 2/3 - ZTC_TDKP_CEO_ALL / ZTC_TDKP_CEO_KH: nhập tay, import Excel (kiểm tra danh mục), extract Excel; KH thêm "chuyển từ CEO_ALL". */
@Service
public class AuditTdkpCeoRecommendationService {

    private final AuditTdkpCeoRecommendationRepository repository;
    private final TdkpMasterData masterData;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    public AuditTdkpCeoRecommendationService(AuditTdkpCeoRecommendationRepository repository, TdkpMasterData masterData,
                                             AuditLogService auditLogService, ExcelExportService excelExportService,
                                             ExcelImportService excelImportService) {
        this.repository = repository;
        this.masterData = masterData;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditTdkpCeoRecommendationDto.Response> list(TdkpCeoScope scope) {
        List<AuditTdkpCeoRecommendation> items = repository.findByTenantIdAndScopeOrderByReportDateDescCreatedAtDesc(TenantContext.getTenantId(), scope);
        Lookup lookup = lookup();
        return items.stream().map(item -> toResponse(item, lookup)).toList();
    }

    @Transactional
    public AuditTdkpCeoRecommendationDto.Response create(TdkpCeoScope scope, AuditTdkpCeoRecommendationDto.Request request) {
        AuditTdkpCeoRecommendation item = new AuditTdkpCeoRecommendation();
        item.setTenantId(TenantContext.getTenantId());
        item.setScope(scope);
        apply(item, request);
        item = repository.save(item);
        auditLogService.record("AuditTdkpCeoRecommendation", item.getId(), AuditAction.CREATE, "Tao kien nghi HDTV/TGD (" + scope + ")");
        return toResponse(item, lookup());
    }

    @Transactional
    public AuditTdkpCeoRecommendationDto.Response update(TdkpCeoScope scope, UUID id, AuditTdkpCeoRecommendationDto.Request request) {
        AuditTdkpCeoRecommendation item = getOwnedOrThrow(scope, id);
        apply(item, request);
        item = repository.save(item);
        auditLogService.record("AuditTdkpCeoRecommendation", item.getId(), AuditAction.UPDATE, "Cap nhat kien nghi HDTV/TGD (" + scope + ")");
        return toResponse(item, lookup());
    }

    @Transactional
    public void delete(TdkpCeoScope scope, UUID id) {
        AuditTdkpCeoRecommendation item = getOwnedOrThrow(scope, id);
        repository.delete(item);
        auditLogService.record("AuditTdkpCeoRecommendation", id, AuditAction.DELETE, "Xoa kien nghi HDTV/TGD (" + scope + ")");
    }

    /** ZTC_TDKP_CEO_KH: "Cho phép chuyển dữ liệu kiến nghị từ ZTC_TDKP_CEO_ALL" - chuyển các dòng ALL chưa được chuyển (không chuyển trùng). */
    @Transactional
    public AuditTdkpCeoRecommendationDto.TransferResult transferFromAll() {
        UUID tenantId = TenantContext.getTenantId();
        Set<UUID> alreadyTransferred = new HashSet<>();
        for (AuditTdkpCeoRecommendation kh : repository.findByTenantIdAndScopeOrderByReportDateDescCreatedAtDesc(tenantId, TdkpCeoScope.KH)) {
            if (kh.getSourceId() != null) {
                alreadyTransferred.add(kh.getSourceId());
            }
        }
        int transferred = 0;
        int skipped = 0;
        for (AuditTdkpCeoRecommendation all : repository.findByTenantIdAndScopeOrderByReportDateDescCreatedAtDesc(tenantId, TdkpCeoScope.ALL)) {
            if (alreadyTransferred.contains(all.getId())) {
                skipped++;
                continue;
            }
            AuditTdkpCeoRecommendation copy = new AuditTdkpCeoRecommendation();
            copy.setTenantId(tenantId);
            copy.setScope(TdkpCeoScope.KH);
            copy.setSourceId(all.getId());
            copy.setReportNumber(all.getReportNumber());
            copy.setReportDate(all.getReportDate());
            copy.setContent(all.getContent());
            copy.setRecommendationTypeId(all.getRecommendationTypeId());
            copy.setBusinessSegmentId(all.getBusinessSegmentId());
            copy.setTargetObject(all.getTargetObject());
            copy.setExecutingUnitId(all.getExecutingUnitId());
            // cac cot NSD nhap (chi dao/thoi han/hien trang/danh gia/ghi chu) cung duoc mang sang de Phong Ke hoach chi sua tiep
            copy.setDirective(all.getDirective());
            copy.setDeadline(all.getDeadline());
            copy.setStatus(all.getStatus());
            copy.setEvaluation(all.getEvaluation());
            copy.setNote(all.getNote());
            repository.save(copy);
            transferred++;
        }
        auditLogService.record("AuditTdkpCeoRecommendation", null, AuditAction.CREATE,
                "Chuyen kien nghi tu ZTC_TDKP_CEO_ALL sang ZTC_TDKP_CEO_KH: " + transferred + " dong, bo qua " + skipped + " dong da chuyen");
        return new AuditTdkpCeoRecommendationDto.TransferResult(transferred, skipped);
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(TdkpCeoScope scope) {
        List<Map<String, Object>> rows = list(scope).stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("reportNumber", r.reportNumber());
            row.put("reportDate", r.reportDate());
            row.put("content", r.content());
            row.put("recommendationType", r.recommendationTypeName());
            row.put("businessSegment", r.businessSegmentCode());
            row.put("target", r.targetObjectLabel());
            row.put("executingUnit", r.executingUnitName());
            row.put("directive", r.directive());
            row.put("deadline", r.deadline());
            row.put("status", r.statusLabel());
            row.put("evaluation", r.evaluation());
            row.put("deadlineState", r.deadlineStateLabel());
            row.put("lastEditedDate", r.lastEditedDate());
            row.put("lastEditedBy", r.lastEditedBy());
            row.put("note", r.note());
            return row;
        }).toList();
        return excelExportService.export(scope == TdkpCeoScope.ALL ? "TDKP_CEO_ALL" : "TDKP_CEO_KH", columns(scope), rows);
    }

    @Transactional
    public ImportResult importFromExcel(TdkpCeoScope scope, MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), columns(scope));
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }
        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                if (TdkpSupport.isBlank(row.get("content"))) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Noi dung kien nghi");
                }
                // cac cot dang List phai co trong danh muc (BusinessException -> loi dong, khong ghi nhan dong do)
                AuditTdkpCeoRecommendationDto.Request request = new AuditTdkpCeoRecommendationDto.Request(
                        TdkpSupport.emptyToNull(row.get("reportNumber")), TdkpSupport.parseDate(row.get("reportDate")), row.get("content").trim(),
                        masterData.resolveItem(AuditMasterDataCategory.RECOMMENDATION_TYPE, row.get("recommendationType"), "Phan loai kien nghi"),
                        masterData.resolveItem(AuditMasterDataCategory.BUSINESS_SEGMENT, row.get("businessSegment"), "Ma mang nghiep vu"),
                        TdkpTarget.parse(row.get("target")), masterData.resolveUnit(row.get("executingUnit"), "Don vi thuc hien"),
                        TdkpSupport.emptyToNull(row.get("directive")), TdkpSupport.parseDate(row.get("deadline")), TdkpStatus.parse(row.get("status")),
                        TdkpSupport.emptyToNull(row.get("evaluation")), TdkpSupport.emptyToNull(row.get("note")));
                create(scope, request);
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(i + 2, e.getMessage()));
            }
        }
        auditLogService.record("AuditTdkpCeoRecommendation", null, AuditAction.CREATE,
                "Import Excel kien nghi HDTV/TGD (" + scope + "): " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private List<ExportColumn> columns(TdkpCeoScope scope) {
        List<ExportColumn> columns = new ArrayList<>(List.of(new ExportColumn("reportNumber", "Số báo cáo"), new ExportColumn("reportDate", "Ngày báo cáo"),
                new ExportColumn("content", "Nội dung kiến nghị"), new ExportColumn("recommendationType", "Phân loại kiến nghị"),
                new ExportColumn("businessSegment", "Mã mảng nghiệp vụ"), new ExportColumn("target", "Đối tượng được kiến nghị"),
                new ExportColumn("executingUnit", "Đơn vị thực hiện"), new ExportColumn("directive", "Chỉ đạo của HĐTV, TGĐ"),
                new ExportColumn("deadline", "Thời hạn hoàn thành"), new ExportColumn("status", "Hiện trạng"),
                new ExportColumn("evaluation", "Đánh giá tình hình thực hiện kiến nghị của Phòng nghiệp vụ"),
                new ExportColumn("deadlineState", "Trạng thái")));
        if (scope == TdkpCeoScope.ALL) {
            columns.add(new ExportColumn("lastEditedDate", "Ngày thực hiện chỉnh sửa"));
        }
        columns.add(new ExportColumn("lastEditedBy", "User chỉnh sửa"));
        columns.add(new ExportColumn("note", "Ghi chú"));
        return columns;
    }

    private void apply(AuditTdkpCeoRecommendation item, AuditTdkpCeoRecommendationDto.Request request) {
        item.setReportNumber(TdkpSupport.emptyToNull(request.reportNumber()));
        item.setReportDate(request.reportDate());
        item.setContent(request.content().trim());
        item.setRecommendationTypeId(masterData.requireItem(AuditMasterDataCategory.RECOMMENDATION_TYPE, request.recommendationTypeId(), "Phan loai kien nghi"));
        item.setBusinessSegmentId(masterData.requireItem(AuditMasterDataCategory.BUSINESS_SEGMENT, request.businessSegmentId(), "Ma mang nghiep vu"));
        item.setTargetObject(request.targetObject());
        item.setExecutingUnitId(masterData.requireUnit(request.executingUnitId(), "Don vi thuc hien"));
        item.setDirective(TdkpSupport.emptyToNull(request.directive()));
        item.setDeadline(request.deadline());
        item.setStatus(request.status());
        item.setEvaluation(TdkpSupport.emptyToNull(request.evaluation()));
        item.setNote(TdkpSupport.emptyToNull(request.note()));
    }

    private AuditTdkpCeoRecommendation getOwnedOrThrow(TdkpCeoScope scope, UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findById(id).filter(i -> i.getTenantId().equals(tenantId) && i.getScope() == scope)
                .orElseThrow(() -> new BusinessException("TDKP_CEO_RECOMMENDATION_NOT_FOUND", "Khong tim thay kien nghi", HttpStatus.NOT_FOUND));
    }

    private record Lookup(Map<UUID, AuditMasterDataItem> types, Map<UUID, AuditMasterDataItem> segments, Map<UUID, AuditObjectUnit> units) {
    }

    private Lookup lookup() {
        return new Lookup(masterData.items(AuditMasterDataCategory.RECOMMENDATION_TYPE), masterData.items(AuditMasterDataCategory.BUSINESS_SEGMENT),
                masterData.units());
    }

    private AuditTdkpCeoRecommendationDto.Response toResponse(AuditTdkpCeoRecommendation item, Lookup lookup) {
        AuditObjectUnit unit = item.getExecutingUnitId() == null ? null : lookup.units().get(item.getExecutingUnitId());
        String state = TdkpSupport.deadlineState(item.getDeadline());
        java.time.Instant editedAt = item.getUpdatedAt() != null ? item.getUpdatedAt() : item.getCreatedAt();
        LocalDate editedDate = editedAt == null ? null : editedAt.atZone(ZoneId.systemDefault()).toLocalDate();
        String editedBy = item.getUpdatedBy() != null ? item.getUpdatedBy() : item.getCreatedBy();
        return new AuditTdkpCeoRecommendationDto.Response(item.getId(), item.getScope(), item.getSourceId(), item.getReportNumber(), item.getReportDate(),
                item.getContent(), item.getRecommendationTypeId(), TdkpMasterData.nameOf(lookup.types(), item.getRecommendationTypeId()),
                item.getBusinessSegmentId(), TdkpMasterData.codeOf(lookup.segments(), item.getBusinessSegmentId()), item.getTargetObject(),
                TdkpTarget.labelOf(item.getTargetObject()), item.getExecutingUnitId(), unit == null ? null : unit.getName(), item.getDirective(),
                item.getDeadline(), item.getStatus(), TdkpStatus.labelOf(item.getStatus()), item.getEvaluation(), state, TdkpSupport.deadlineLabel(state),
                editedDate, editedBy, item.getNote());
    }
}
