package com.govia.audit.cmntd1.service;

import com.govia.audit.cmntd1.dto.AuditCmNtd1Request;
import com.govia.audit.cmntd1.dto.AuditCmNtd1Response;
import com.govia.audit.cmntd1.entity.AuditCmNtd1;
import com.govia.audit.cmntd1.repository.AuditCmNtd1Repository;
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

/** CRUD + Import/Export cho "Danh sach cac but toan chon mau TCKT" (sheet ZTC_CM_NTD1). */
@Service
public class AuditCmNtd1Service {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
    };

    private final AuditCmNtd1Repository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmNtd1Service(AuditCmNtd1Repository repository, AuditLogService auditLogService,
                               ExcelExportService excelExportService, WordExportService wordExportService,
                               ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditCmNtd1Response> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditCmNtd1Response create(AuditCmNtd1Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.transactionDate(), request.postingUser(), request.entryNumber(), null);

        AuditCmNtd1 item = new AuditCmNtd1();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd1", item.getId(), AuditAction.CREATE,
                "Tao ban ghi chon mau but toan TCKT: " + item.getBranchCode() + " - " + item.getEntryNumber());
        return toResponse(item);
    }

    @Transactional
    public AuditCmNtd1Response update(UUID id, AuditCmNtd1Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd1 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.transactionDate(), request.postingUser(), request.entryNumber(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd1", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi chon mau but toan TCKT: " + item.getBranchCode() + " - " + item.getEntryNumber());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd1 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmNtd1", id, AuditAction.DELETE,
                "Xoa ban ghi chon mau but toan TCKT: " + item.getBranchCode() + " - " + item.getEntryNumber());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_cm_ntd1", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Danh sách các bút toán chọn mẫu TCKT", exportColumns(), exportRows());
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
                String postingUser = row.get("postingUser");
                LocalDate transactionDate = parseDate(row.get("transactionDate"));
                BigDecimal entryNumber = parseDecimal(row.get("entryNumber"));
                if (isBlank(branchCode) || isBlank(postingUser) || transactionDate == null || entryNumber == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh, User hach toan, Ngay giao dich thuc te hoac So but toan");
                }

                Optional<AuditCmNtd1> existing = repository.findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndEntryNumber(
                        tenantId, branchCode.trim(), transactionDate, postingUser.trim(), entryNumber);
                AuditCmNtd1Request request = new AuditCmNtd1Request(branchCode.trim(), transactionDate, postingUser.trim(), entryNumber,
                        parseDecimal(row.get("debitAmount")), parseDecimal(row.get("creditAmount")), emptyToNull(row.get("transactionStatus")),
                        emptyToNull(row.get("currency")), emptyToNull(row.get("accountNumber")), emptyToNull(row.get("content")),
                        emptyToNull(row.get("sampleReason")), emptyToNull(row.get("auditResult")), emptyToNull(row.get("recommendationType")),
                        emptyToNull(row.get("transactionStaff")), emptyToNull(row.get("controlUser")), emptyToNull(row.get("controlStaff")),
                        emptyToNull(row.get("controlStaffTitle")), emptyToNull(row.get("workType")),
                        existing.map(AuditCmNtd1::isActive).orElse(true));
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

        auditLogService.record("AuditCmNtd1", null, AuditAction.CREATE,
                "Import Excel chon mau but toan TCKT: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditCmNtd1 item, AuditCmNtd1Request request) {
        item.setBranchCode(request.branchCode());
        item.setTransactionDate(request.transactionDate());
        item.setPostingUser(request.postingUser());
        item.setEntryNumber(request.entryNumber());
        item.setDebitAmount(request.debitAmount());
        item.setCreditAmount(request.creditAmount());
        item.setTransactionStatus(request.transactionStatus());
        item.setCurrency(request.currency());
        item.setAccountNumber(request.accountNumber());
        item.setContent(request.content());
        item.setSampleReason(request.sampleReason());
        item.setAuditResult(request.auditResult());
        item.setRecommendationType(request.recommendationType());
        item.setTransactionStaff(request.transactionStaff());
        item.setControlUser(request.controlUser());
        item.setControlStaff(request.controlStaff());
        item.setControlStaffTitle(request.controlStaffTitle());
        item.setWorkType(request.workType());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, BigDecimal entryNumber, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndEntryNumber(tenantId, branchCode, transactionDate, postingUser, entryNumber)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_NTD1_DUPLICATE", "Ban ghi nay da ton tai: " + entryNumber);
                });
    }

    private AuditCmNtd1 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_NTD1_NOT_FOUND", "Khong tim thay ban ghi chon mau but toan TCKT", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("transactionDate", "Ngày giao dịch thực tế"),
                new ExportColumn("postingUser", "User hạch toán"),
                new ExportColumn("entryNumber", "Số bút toán"),
                new ExportColumn("debitAmount", "Số phát sinh nợ"),
                new ExportColumn("creditAmount", "Số phát sinh có"),
                new ExportColumn("transactionStatus", "Trạng thái giao dịch"),
                new ExportColumn("currency", "Loại tiền tệ"),
                new ExportColumn("accountNumber", "Tài khoản hạch toán"),
                new ExportColumn("content", "Nội dung"),
                new ExportColumn("sampleReason", "Lý do chọn mẫu"),
                new ExportColumn("auditResult", "Kết quả kiểm toán"),
                new ExportColumn("recommendationType", "Dạng kiến nghị"),
                new ExportColumn("transactionStaff", "Cán bộ giao dịch"),
                new ExportColumn("controlUser", "User kiểm soát"),
                new ExportColumn("controlStaff", "Cán bộ kiểm soát"),
                new ExportColumn("controlStaffTitle", "Chức danh người kiểm soát"),
                new ExportColumn("workType", "Loại hình CV"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("branchCode", item.getBranchCode());
                    row.put("transactionDate", item.getTransactionDate() == null ? null : item.getTransactionDate().format(DATE_FORMATS[0]));
                    row.put("postingUser", item.getPostingUser());
                    row.put("entryNumber", item.getEntryNumber());
                    row.put("debitAmount", item.getDebitAmount());
                    row.put("creditAmount", item.getCreditAmount());
                    row.put("transactionStatus", item.getTransactionStatus());
                    row.put("currency", item.getCurrency());
                    row.put("accountNumber", item.getAccountNumber());
                    row.put("content", item.getContent());
                    row.put("sampleReason", item.getSampleReason());
                    row.put("auditResult", item.getAuditResult());
                    row.put("recommendationType", item.getRecommendationType());
                    row.put("transactionStaff", item.getTransactionStaff());
                    row.put("controlUser", item.getControlUser());
                    row.put("controlStaff", item.getControlStaff());
                    row.put("controlStaffTitle", item.getControlStaffTitle());
                    row.put("workType", item.getWorkType());
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

    private AuditCmNtd1Response toResponse(AuditCmNtd1 item) {
        return new AuditCmNtd1Response(item.getId(), item.getBranchCode(), item.getTransactionDate(), item.getPostingUser(),
                item.getEntryNumber(), item.getDebitAmount(), item.getCreditAmount(), item.getTransactionStatus(),
                item.getCurrency(), item.getAccountNumber(), item.getContent(), item.getSampleReason(), item.getAuditResult(),
                item.getRecommendationType(), item.getTransactionStaff(), item.getControlUser(), item.getControlStaff(),
                item.getControlStaffTitle(), item.getWorkType(), item.isActive());
    }
}
