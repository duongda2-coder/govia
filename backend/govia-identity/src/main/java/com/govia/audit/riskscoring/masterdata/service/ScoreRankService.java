package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.ScoreRankRequest;
import com.govia.audit.riskscoring.masterdata.dto.ScoreRankResponse;
import com.govia.audit.riskscoring.masterdata.entity.RiskScoreRank;
import com.govia.audit.riskscoring.masterdata.repository.RiskScoreRankRepository;
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

/**
 * CRUD + Import/Export cho danh muc "Thang diem xep loai rui ro" (sheet "QL thang diem", tcode
 * ztc_rank). Theo dung mo ta trong tai lieu goc: khi them 1 ky moi (fromYear lon hon) cho CUNG 1
 * xep loai, ky cu dang mo (toYear=9999) se tu dong duoc dong lai = fromYear moi - 1.
 */
@Service
public class ScoreRankService {

    private static final int OPEN_END_YEAR = 9999;

    private final RiskScoreRankRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public ScoreRankService(RiskScoreRankRepository repository, AuditLogService auditLogService,
                             ExcelExportService excelExportService, WordExportService wordExportService,
                             ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<ScoreRankResponse> list() {
        return repository.findByTenantIdOrderByFromYearAscScoreFromAsc(TenantContext.getTenantId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ScoreRankResponse create(ScoreRankRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        int toYear = request.toYear() != null ? request.toYear() : OPEN_END_YEAR;
        checkNoDuplicate(tenantId, request.rankLabel(), request.fromYear(), null);
        closePreviousOpenPeriod(tenantId, request.rankLabel(), request.fromYear());

        RiskScoreRank item = new RiskScoreRank();
        item.setTenantId(tenantId);
        applyRequest(item, request, toYear);
        item = repository.save(item);

        auditLogService.record("RiskScoreRank", item.getId(), AuditAction.CREATE, "Tao thang diem xep loai: " + item.getRankLabel());
        return toResponse(item);
    }

    @Transactional
    public ScoreRankResponse update(UUID id, ScoreRankRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskScoreRank item = getOwnedOrThrow(tenantId, id);
        int toYear = request.toYear() != null ? request.toYear() : OPEN_END_YEAR;
        checkNoDuplicate(tenantId, request.rankLabel(), request.fromYear(), id);

        applyRequest(item, request, toYear);
        item = repository.save(item);

        auditLogService.record("RiskScoreRank", item.getId(), AuditAction.UPDATE, "Cap nhat thang diem xep loai: " + item.getRankLabel());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskScoreRank item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskScoreRank", id, AuditAction.DELETE, "Xoa thang diem xep loai: " + item.getRankLabel());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_rank", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Thang điểm xếp loại rủi ro", exportColumns(), exportRows());
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
                BigDecimal scoreFrom = parseDecimal(row.get("scoreFrom"));
                BigDecimal scoreTo = parseDecimal(row.get("scoreTo"));
                String rankLabel = row.get("rankLabel");
                Integer fromYear = parseInt(row.get("fromYear"));
                if (scoreFrom == null || scoreTo == null || isBlank(rankLabel) || fromYear == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Gia tri bat dau/ket thuc, Xep loai hoac Nam bat dau");
                }
                create(new ScoreRankRequest(scoreFrom, scoreTo, rankLabel.trim(), fromYear, parseInt(row.get("toYear")), true));
                success++;
            } catch (BusinessException e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskScoreRank", null, AuditAction.CREATE,
                "Import Excel thang diem xep loai: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    /** Dong ky cu dang mo (toYear=9999) cua cung 1 xep loai khi ky moi bat dau muon hon, theo dung mo ta trong tai lieu goc. */
    private void closePreviousOpenPeriod(UUID tenantId, String rankLabel, Integer newFromYear) {
        repository.findByTenantIdAndRankLabelAndToYear(tenantId, rankLabel, OPEN_END_YEAR).stream()
                .filter(previous -> previous.getFromYear() < newFromYear)
                .forEach(previous -> {
                    previous.setToYear(newFromYear - 1);
                    repository.save(previous);
                });
    }

    private void applyRequest(RiskScoreRank item, ScoreRankRequest request, int toYear) {
        item.setScoreFrom(request.scoreFrom());
        item.setScoreTo(request.scoreTo());
        item.setRankLabel(request.rankLabel());
        item.setFromYear(request.fromYear());
        item.setToYear(toYear);
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String rankLabel, Integer fromYear, UUID excludingId) {
        repository.findByTenantIdAndRankLabelAndFromYear(tenantId, rankLabel, fromYear)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_SCORE_RANK_DUPLICATE", "Da ton tai xep loai " + rankLabel + " tu nam " + fromYear);
                });
    }

    private RiskScoreRank getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_SCORE_RANK_NOT_FOUND", "Khong tim thay thang diem xep loai", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("scoreFrom", "Gia tri bat dau"),
                new ExportColumn("scoreTo", "Gia tri ket thuc"),
                new ExportColumn("rankLabel", "Xep loai"),
                new ExportColumn("fromYear", "Nam bat dau"),
                new ExportColumn("toYear", "Nam ket thuc"));
    }

    private List<Map<String, Object>> exportRows() {
        return repository.findByTenantIdOrderByFromYearAscScoreFromAsc(TenantContext.getTenantId()).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("scoreFrom", item.getScoreFrom());
                    row.put("scoreTo", item.getScoreTo());
                    row.put("rankLabel", item.getRankLabel());
                    row.put("fromYear", item.getFromYear());
                    row.put("toYear", item.getToYear());
                    return row;
                }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    private Integer parseInt(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ScoreRankResponse toResponse(RiskScoreRank item) {
        return new ScoreRankResponse(item.getId(), item.getScoreFrom(), item.getScoreTo(), item.getRankLabel(),
                item.getFromYear(), item.getToYear(), item.isActive());
    }
}
