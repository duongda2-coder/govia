package com.govia.audit.phbc.report;

import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.phbc.common.PhbcMasterData;
import com.govia.audit.phbc.common.PhbcSupport;
import com.govia.audit.phbc.common.ReportIssuingUnit;
import com.govia.audit.phbc.common.ReportRecommendationCode;
import com.govia.audit.phbc.common.ReportRecommendationTarget;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Man hinh "Phat hanh bao cao" (file ztc_phbc.xlsx): danh sach bao cao da phat hanh + kien nghi dinh kem tung bao cao. */
@Service
public class AuditReportIssuanceService {

    private final AuditReportIssuanceRepository reportRepository;
    private final AuditReportIssuanceRecommendationRepository recommendationRepository;
    private final PhbcMasterData masterData;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    public AuditReportIssuanceService(AuditReportIssuanceRepository reportRepository, AuditReportIssuanceRecommendationRepository recommendationRepository,
                                      PhbcMasterData masterData, AuditLogService auditLogService, ExcelExportService excelExportService,
                                      ExcelImportService excelImportService) {
        this.reportRepository = reportRepository;
        this.recommendationRepository = recommendationRepository;
        this.masterData = masterData;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
    }

    // ===================== 1. man hinh Phat hanh bao cao =====================

    @Transactional(readOnly = true)
    public List<AuditReportIssuanceDto.ReportResponse> listReports() {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditReportIssuance> reports = reportRepository.findByTenantIdOrderByReportDateDescReportNumberAsc(tenantId);
        Map<UUID, Long> recommendationCounts = recommendationCounts(tenantId, reports);
        return reports.stream().map(r -> toReportResponse(r, recommendationCounts.getOrDefault(r.getId(), 0L).intValue())).toList();
    }

    @Transactional
    public AuditReportIssuanceDto.ReportResponse createReport(AuditReportIssuanceDto.ReportRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditReportIssuance report = new AuditReportIssuance();
        report.setTenantId(tenantId);
        applyReport(report, request);
        report = reportRepository.save(report);
        auditLogService.record("AuditReportIssuance", report.getId(), AuditAction.CREATE, "Tao bao cao phat hanh: " + report.getReportNumber());
        return toReportResponse(report, 0);
    }

    @Transactional
    public AuditReportIssuanceDto.ReportResponse updateReport(UUID id, AuditReportIssuanceDto.ReportRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditReportIssuance report = getReportOrThrow(id);
        applyReport(report, request);
        report = reportRepository.save(report);
        int count = recommendationRepository.findByTenantIdAndReportIssuanceIdOrderByCreatedAtAsc(tenantId, id).size();
        auditLogService.record("AuditReportIssuance", report.getId(), AuditAction.UPDATE, "Cap nhat bao cao phat hanh: " + report.getReportNumber());
        return toReportResponse(report, count);
    }

    @Transactional
    public void deleteReport(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditReportIssuance report = getReportOrThrow(id);
        recommendationRepository.deleteByTenantIdAndReportIssuanceId(tenantId, id);
        reportRepository.delete(report);
        auditLogService.record("AuditReportIssuance", id, AuditAction.DELETE, "Xoa bao cao phat hanh: " + report.getReportNumber());
    }

    // ===================== 2. kien nghi trong bao cao =====================

    /** reportIssuanceId null -> toan bo kien nghi. */
    @Transactional(readOnly = true)
    public List<AuditReportIssuanceDto.RecommendationResponse> listRecommendations(UUID reportIssuanceId) {
        UUID tenantId = TenantContext.getTenantId();
        List<AuditReportIssuance> reports = reportRepository.findByTenantIdOrderByReportDateDescReportNumberAsc(tenantId);
        Map<UUID, AuditReportIssuance> reportById = reports.stream().collect(Collectors.toMap(AuditReportIssuance::getId, r -> r));
        Lookup lookup = lookup();
        List<AuditReportIssuanceRecommendation> items = reportIssuanceId != null
                ? recommendationRepository.findByTenantIdAndReportIssuanceIdOrderByCreatedAtAsc(tenantId, reportIssuanceId)
                : recommendationRepository.findByTenantIdAndReportIssuanceIdIn(tenantId, reportById.keySet());
        return items.stream().map(item -> toRecommendationResponse(item, reportById.get(item.getReportIssuanceId()), lookup)).toList();
    }

    @Transactional
    public AuditReportIssuanceDto.RecommendationResponse createRecommendation(AuditReportIssuanceDto.RecommendationRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditReportIssuance report = getReportOrThrow(request.reportIssuanceId());
        AuditReportIssuanceRecommendation item = new AuditReportIssuanceRecommendation();
        item.setTenantId(tenantId);
        applyRecommendation(item, request);
        item = recommendationRepository.save(item);
        auditLogService.record("AuditReportIssuanceRecommendation", item.getId(), AuditAction.CREATE,
                "Them kien nghi vao bao cao " + report.getReportNumber());
        return toRecommendationResponse(item, report, lookup());
    }

    @Transactional
    public AuditReportIssuanceDto.RecommendationResponse updateRecommendation(UUID id, AuditReportIssuanceDto.RecommendationRequest request) {
        AuditReportIssuanceRecommendation item = getRecommendationOrThrow(id);
        if (!item.getReportIssuanceId().equals(request.reportIssuanceId())) {
            throw new BusinessException("PHBC_RECOMMENDATION_MOVE_NOT_ALLOWED", "Khong doi bao cao cua dong kien nghi");
        }
        AuditReportIssuance report = getReportOrThrow(request.reportIssuanceId());
        applyRecommendation(item, request);
        item = recommendationRepository.save(item);
        auditLogService.record("AuditReportIssuanceRecommendation", item.getId(), AuditAction.UPDATE, "Cap nhat kien nghi cua bao cao " + report.getReportNumber());
        return toRecommendationResponse(item, report, lookup());
    }

    @Transactional
    public void deleteRecommendation(UUID id) {
        AuditReportIssuanceRecommendation item = getRecommendationOrThrow(id);
        recommendationRepository.delete(item);
        auditLogService.record("AuditReportIssuanceRecommendation", id, AuditAction.DELETE, "Xoa kien nghi khoi bao cao phat hanh");
    }

    // ===================== Excel =====================

    private List<ExportColumn> reportColumns() {
        return List.of(new ExportColumn("reportNumber", "Số báo cáo"), new ExportColumn("reportDate", "Ngày báo cáo"),
                new ExportColumn("reportName", "Tên báo cáo"), new ExportColumn("summaryContent", "Nội dung trích yếu"),
                new ExportColumn("issuingUnit", "Đơn vị phát hành"));
    }

    @Transactional(readOnly = true)
    public byte[] exportReports() {
        List<Map<String, Object>> rows = listReports().stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("reportNumber", r.reportNumber());
            row.put("reportDate", r.reportDate());
            row.put("reportName", r.reportName());
            row.put("summaryContent", r.summaryContent());
            row.put("issuingUnit", r.issuingUnitLabel());
            return row;
        }).toList();
        return excelExportService.export("PHBC_BAO_CAO", reportColumns(), rows);
    }

    /** Co So bao cao da ton tai thi cap nhat dong do, nguoc lai tao moi (khoa nghiep vu = So bao cao). */
    @Transactional
    public ImportResult importReports(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), reportColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }
        UUID tenantId = TenantContext.getTenantId();
        Map<String, AuditReportIssuance> byNumber = reportRepository.findByTenantIdOrderByReportDateDescReportNumberAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditReportIssuance::getReportNumber, r -> r, (a, b) -> a));
        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                if (PhbcSupport.isBlank(row.get("reportNumber"))) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu So bao cao");
                }
                if (PhbcSupport.isBlank(row.get("reportName"))) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ten bao cao");
                }
                AuditReportIssuanceDto.ReportRequest request = new AuditReportIssuanceDto.ReportRequest(row.get("reportNumber").trim(),
                        PhbcSupport.parseDate(row.get("reportDate")), row.get("reportName").trim(), PhbcSupport.emptyToNull(row.get("summaryContent")),
                        ReportIssuingUnit.parse(row.get("issuingUnit")));
                AuditReportIssuance existing = byNumber.get(request.reportNumber());
                if (existing != null) {
                    updateReport(existing.getId(), request);
                } else {
                    AuditReportIssuanceDto.ReportResponse created = createReport(request);
                    AuditReportIssuance saved = getReportOrThrow(created.id());
                    byNumber.put(saved.getReportNumber(), saved);
                }
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(i + 2, e.getMessage()));
            }
        }
        auditLogService.record("AuditReportIssuance", null, AuditAction.CREATE, "Import Excel bao cao phat hanh: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private List<ExportColumn> recommendationColumns() {
        return List.of(new ExportColumn("reportNumber", "Số báo cáo"), new ExportColumn("recommendationCode", "Mã kiến nghị"),
                new ExportColumn("recommendationName", "Tên KN"), new ExportColumn("content", "Nội dung Kiến nghị"),
                new ExportColumn("businessSegment", "Mảng nghiệp vụ"), new ExportColumn("target", "Đối tượng kiến nghị"),
                new ExportColumn("executingUnit", "Đơn vị thực hiện"), new ExportColumn("executingUnitName", "Tên đơn vị thực hiện"),
                new ExportColumn("deadline", "Thời hạn hoàn thành"));
    }

    @Transactional(readOnly = true)
    public byte[] exportRecommendations() {
        List<Map<String, Object>> rows = listRecommendations(null).stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("reportNumber", r.reportNumber());
            row.put("recommendationCode", r.recommendationCode());
            row.put("recommendationName", r.recommendationName());
            row.put("content", r.content());
            row.put("businessSegment", r.businessSegmentCode());
            row.put("target", ReportRecommendationTarget.labelOf(r.target()));
            row.put("executingUnit", r.executingUnitCode());
            row.put("executingUnitName", r.executingUnitName());
            row.put("deadline", r.deadline());
            return row;
        }).toList();
        return excelExportService.export("PHBC_KIEN_NGHI", recommendationColumns(), rows);
    }

    /** Import kien nghi: phai co "So bao cao" khop 1 bao cao da co san (khong tu tao bao cao moi tu dong Import nay). */
    @Transactional
    public ImportResult importRecommendations(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), recommendationColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }
        UUID tenantId = TenantContext.getTenantId();
        Map<String, AuditReportIssuance> byNumber = reportRepository.findByTenantIdOrderByReportDateDescReportNumberAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditReportIssuance::getReportNumber, r -> r, (a, b) -> a));
        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                AuditReportIssuance report = byNumber.get(PhbcSupport.emptyToNull(row.get("reportNumber")));
                if (report == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Khong tim thay So bao cao: " + row.get("reportNumber"));
                }
                if (PhbcSupport.isBlank(row.get("content"))) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Noi dung Kien nghi");
                }
                AuditReportIssuanceDto.RecommendationRequest request = new AuditReportIssuanceDto.RecommendationRequest(report.getId(),
                        ReportRecommendationCode.parse(row.get("recommendationCode")), row.get("content").trim(),
                        masterData.resolveBusinessSegment(row.get("businessSegment")), ReportRecommendationTarget.parse(row.get("target")),
                        masterData.resolveExecutingUnit(row.get("executingUnit")), PhbcSupport.emptyToNull(row.get("executingUnitName")),
                        PhbcSupport.parseDate(row.get("deadline")));
                createRecommendation(request);
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(i + 2, e.getMessage()));
            }
        }
        auditLogService.record("AuditReportIssuanceRecommendation", null, AuditAction.CREATE,
                "Import Excel kien nghi bao cao phat hanh: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    // ===================== noi bo =====================

    private void applyReport(AuditReportIssuance report, AuditReportIssuanceDto.ReportRequest request) {
        report.setReportNumber(request.reportNumber().trim());
        report.setReportDate(request.reportDate());
        report.setReportName(request.reportName().trim());
        report.setSummaryContent(PhbcSupport.emptyToNull(request.summaryContent()));
        report.setIssuingUnit(request.issuingUnit());
    }

    private void applyRecommendation(AuditReportIssuanceRecommendation item, AuditReportIssuanceDto.RecommendationRequest request) {
        item.setReportIssuanceId(request.reportIssuanceId());
        item.setRecommendationCode(request.recommendationCode());
        item.setContent(request.content().trim());
        item.setBusinessSegmentId(masterData.requireBusinessSegment(request.businessSegmentId()));
        item.setTarget(request.target());
        item.setExecutingUnitId(masterData.requireExecutingUnit(request.executingUnitId()));
        item.setExecutingUnitName(PhbcSupport.emptyToNull(request.executingUnitName()));
        item.setDeadline(request.deadline());
    }

    private Map<UUID, Long> recommendationCounts(UUID tenantId, List<AuditReportIssuance> reports) {
        if (reports.isEmpty()) {
            return Map.of();
        }
        return recommendationRepository.findByTenantIdAndReportIssuanceIdIn(tenantId, reports.stream().map(AuditReportIssuance::getId).toList())
                .stream().collect(Collectors.groupingBy(AuditReportIssuanceRecommendation::getReportIssuanceId, Collectors.counting()));
    }

    private AuditReportIssuance getReportOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return reportRepository.findById(id).filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("PHBC_REPORT_NOT_FOUND", "Khong tim thay bao cao phat hanh", HttpStatus.NOT_FOUND));
    }

    private AuditReportIssuanceRecommendation getRecommendationOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return recommendationRepository.findById(id).filter(i -> i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("PHBC_RECOMMENDATION_NOT_FOUND", "Khong tim thay kien nghi", HttpStatus.NOT_FOUND));
    }

    private record Lookup(Map<UUID, AuditMasterDataItem> segments, Map<UUID, AuditObjectUnit> units) {
    }

    private Lookup lookup() {
        return new Lookup(masterData.businessSegments(), masterData.executingUnits());
    }

    private AuditReportIssuanceDto.ReportResponse toReportResponse(AuditReportIssuance report, int recommendationCount) {
        return new AuditReportIssuanceDto.ReportResponse(report.getId(), report.getReportNumber(), report.getReportDate(), report.getReportName(),
                report.getSummaryContent(), report.getIssuingUnit(), ReportIssuingUnit.labelOf(report.getIssuingUnit()), recommendationCount);
    }

    private AuditReportIssuanceDto.RecommendationResponse toRecommendationResponse(AuditReportIssuanceRecommendation item, AuditReportIssuance report, Lookup lookup) {
        AuditMasterDataItem segment = item.getBusinessSegmentId() == null ? null : lookup.segments().get(item.getBusinessSegmentId());
        AuditObjectUnit unit = item.getExecutingUnitId() == null ? null : lookup.units().get(item.getExecutingUnitId());
        return new AuditReportIssuanceDto.RecommendationResponse(item.getId(), item.getReportIssuanceId(),
                report == null ? null : report.getReportNumber(), item.getRecommendationCode(), ReportRecommendationCode.labelOf(item.getRecommendationCode()),
                item.getContent(), item.getBusinessSegmentId(), segment == null ? null : segment.getCode(), segment == null ? null : segment.getName(),
                item.getTarget(), ReportRecommendationTarget.labelOf(item.getTarget()), item.getExecutingUnitId(), unit == null ? null : unit.getCode(),
                item.getExecutingUnitName(), item.getDeadline());
    }
}
