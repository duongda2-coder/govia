package com.govia.audit.cmntd14.service;

import com.govia.audit.cmntd14.dto.AuditCmNtd14Request;
import com.govia.audit.cmntd14.dto.AuditCmNtd14Response;
import com.govia.audit.cmntd14.entity.AuditCmNtd14;
import com.govia.audit.cmntd14.repository.AuditCmNtd14Repository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** CRUD + Import/Export cho "Danh sách chọn mẫu User Ipcas chấm công (08C)" (sheet ZTC_CM_NTD14). */
@Service
public class AuditCmNtd14Service {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
    };

    private final AuditCmNtd14Repository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmNtd14Service(AuditCmNtd14Repository repository, AuditLogService auditLogService,
                                ExcelExportService excelExportService, WordExportService wordExportService,
                                ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditCmNtd14Response> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditCmNtd14Response create(AuditCmNtd14Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.attendanceDate(), request.staffCode(), null);

        AuditCmNtd14 item = new AuditCmNtd14();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd14", item.getId(), AuditAction.CREATE,
                "Tao ban ghi chon mau cham cong: " + item.getBranchCode() + " - " + item.getStaffCode());
        return toResponse(item);
    }

    @Transactional
    public AuditCmNtd14Response update(UUID id, AuditCmNtd14Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd14 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.attendanceDate(), request.staffCode(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd14", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi chon mau cham cong: " + item.getBranchCode() + " - " + item.getStaffCode());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd14 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmNtd14", id, AuditAction.DELETE,
                "Xoa ban ghi chon mau cham cong: " + item.getBranchCode() + " - " + item.getStaffCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_cm_ntd14", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Danh sách chọn mẫu User Ipcas chấm công (08C)", exportColumns(), exportRows());
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
        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String branchCode = row.get("branchCode");
                String staffCode = row.get("staffCode");
                LocalDate attendanceDate = parseDate(row.get("attendanceDate"));
                if (isBlank(branchCode) || isBlank(staffCode) || attendanceDate == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh, Ngay cham cong hoac Ma can bo");
                }

                Optional<AuditCmNtd14> existing = repository.findByTenantIdAndBranchCodeAndAttendanceDateAndStaffCode(
                        tenantId, branchCode.trim(), attendanceDate, staffCode.trim());
                AuditCmNtd14Request request = new AuditCmNtd14Request(branchCode.trim(), attendanceDate, staffCode.trim(),
                        emptyToNull(row.get("staffName")), emptyToNull(row.get("attendanceCode")), emptyToNull(row.get("description")),
                        parseDecimal(row.get("matchedTransactionCount")), parseDecimal(row.get("unmatchedTransactionCount")),
                        parseDecimal(row.get("adjustedTransactionCount")), emptyToNull(row.get("userCode")), emptyToNull(row.get("note")),
                        emptyToNull(row.get("sampleCode")), emptyToNull(row.get("sampleReason")), emptyToNull(row.get("auditResult")),
                        existing.map(AuditCmNtd14::isActive).orElse(true));
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

        auditLogService.record("AuditCmNtd14", null, AuditAction.CREATE,
                "Import Excel chon mau cham cong: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditCmNtd14 item, AuditCmNtd14Request request) {
        item.setBranchCode(request.branchCode());
        item.setAttendanceDate(request.attendanceDate());
        item.setStaffCode(request.staffCode());
        item.setStaffName(request.staffName());
        item.setAttendanceCode(request.attendanceCode());
        item.setDescription(request.description());
        item.setMatchedTransactionCount(request.matchedTransactionCount());
        item.setUnmatchedTransactionCount(request.unmatchedTransactionCount());
        item.setAdjustedTransactionCount(request.adjustedTransactionCount());
        item.setUserCode(request.userCode());
        item.setNote(request.note());
        item.setSampleCode(request.sampleCode());
        item.setSampleReason(request.sampleReason());
        item.setAuditResult(request.auditResult());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, LocalDate attendanceDate, String staffCode, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndAttendanceDateAndStaffCode(tenantId, branchCode, attendanceDate, staffCode)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_NTD14_DUPLICATE", "Ban ghi nay da ton tai: " + staffCode);
                });
    }

    private AuditCmNtd14 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_NTD14_NOT_FOUND", "Khong tim thay ban ghi chon mau cham cong", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("attendanceDate", "Ngày chấm công"),
                new ExportColumn("staffCode", "Mã cán bộ"),
                new ExportColumn("staffName", "Tên cán bộ"),
                new ExportColumn("attendanceCode", "Chấm công"),
                new ExportColumn("description", "Diễn giải"),
                new ExportColumn("matchedTransactionCount", "Số GD HT"),
                new ExportColumn("unmatchedTransactionCount", "Số GD Ko HT"),
                new ExportColumn("adjustedTransactionCount", "Số GD ĐK, CS TT"),
                new ExportColumn("userCode", "User"),
                new ExportColumn("note", "Ghi chú"),
                new ExportColumn("sampleCode", "Mã mẫu chọn"),
                new ExportColumn("sampleReason", "Lý do chọn mẫu"),
                new ExportColumn("auditResult", "Kết quả kiểm toán"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("branchCode", item.getBranchCode());
                    row.put("attendanceDate", item.getAttendanceDate() == null ? null : item.getAttendanceDate().format(DATE_FORMATS[0]));
                    row.put("staffCode", item.getStaffCode());
                    row.put("staffName", item.getStaffName());
                    row.put("attendanceCode", item.getAttendanceCode());
                    row.put("description", item.getDescription());
                    row.put("matchedTransactionCount", item.getMatchedTransactionCount());
                    row.put("unmatchedTransactionCount", item.getUnmatchedTransactionCount());
                    row.put("adjustedTransactionCount", item.getAdjustedTransactionCount());
                    row.put("userCode", item.getUserCode());
                    row.put("note", item.getNote());
                    row.put("sampleCode", item.getSampleCode());
                    row.put("sampleReason", item.getSampleReason());
                    row.put("auditResult", item.getAuditResult());
                    return row;
                }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** O ngay trong Excel co the la text "10.04.2021" hoac 1 o date-format that (POI tra ve numeric
     * serial number qua ExcelImportServiceImpl.cellToString, xem class do) - thu ca 2 truong hop. */
    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, format);
            } catch (DateTimeParseException ignored) {
                // thu dinh dang tiep theo
            }
        }
        if (trimmed.matches("\\d+(\\.\\d+)?")) {
            try {
                long serial = new BigDecimal(trimmed).longValue();
                return LocalDate.of(1899, 12, 30).plusDays(serial);
            } catch (NumberFormatException | java.time.DateTimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private AuditCmNtd14Response toResponse(AuditCmNtd14 item) {
        return new AuditCmNtd14Response(item.getId(), item.getBranchCode(), item.getAttendanceDate(), item.getStaffCode(),
                item.getStaffName(), item.getAttendanceCode(), item.getDescription(), item.getMatchedTransactionCount(),
                item.getUnmatchedTransactionCount(), item.getAdjustedTransactionCount(), item.getUserCode(), item.getNote(),
                item.getSampleCode(), item.getSampleReason(), item.getAuditResult(), item.isActive());
    }
}
