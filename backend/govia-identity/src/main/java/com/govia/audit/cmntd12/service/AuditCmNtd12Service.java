package com.govia.audit.cmntd12.service;

import com.govia.audit.cmntd12.dto.AuditCmNtd12Request;
import com.govia.audit.cmntd12.dto.AuditCmNtd12Response;
import com.govia.audit.cmntd12.entity.AuditCmNtd12;
import com.govia.audit.cmntd12.repository.AuditCmNtd12Repository;
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

/** CRUD + Import/Export cho "Danh cac but toan chon mau TCKT" (sheet ZTC_CM_NTD12). */
@Service
public class AuditCmNtd12Service {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
    };

    private final AuditCmNtd12Repository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmNtd12Service(AuditCmNtd12Repository repository, AuditLogService auditLogService,
                                ExcelExportService excelExportService, WordExportService wordExportService,
                                ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditCmNtd12Response> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditCmNtd12Response create(AuditCmNtd12Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.transactionDate(), request.postingUser(), request.entryNumber(), null);

        AuditCmNtd12 item = new AuditCmNtd12();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd12", item.getId(), AuditAction.CREATE,
                "Tao ban ghi but toan chon mau TCKT: " + item.getBranchCode() + " - " + item.getEntryNumber());
        return toResponse(item);
    }

    @Transactional
    public AuditCmNtd12Response update(UUID id, AuditCmNtd12Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd12 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.transactionDate(), request.postingUser(), request.entryNumber(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd12", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi but toan chon mau TCKT: " + item.getBranchCode() + " - " + item.getEntryNumber());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd12 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmNtd12", id, AuditAction.DELETE,
                "Xoa ban ghi but toan chon mau TCKT: " + item.getBranchCode() + " - " + item.getEntryNumber());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_cm_ntd12", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Danh các bút toán chọn mẫu TCKT", exportColumns(), exportRows());
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
                BigDecimal entryNumber = parseDecimal(row.get("entryNumber"));
                LocalDate transactionDate = parseDate(row.get("transactionDate"));
                if (isBlank(branchCode) || isBlank(postingUser) || entryNumber == null || transactionDate == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh, User hach toan, So but toan hoac Ngay giao dich thuc te");
                }

                Optional<AuditCmNtd12> existing = repository.findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndEntryNumber(
                        tenantId, branchCode.trim(), transactionDate, postingUser.trim(), entryNumber);
                AuditCmNtd12Request request = new AuditCmNtd12Request(branchCode.trim(), transactionDate, postingUser.trim(), entryNumber,
                        parseDecimal(row.get("debitAmount")), parseDecimal(row.get("creditAmount")), emptyToNull(row.get("transactionStatus")),
                        emptyToNull(row.get("currency")), emptyToNull(row.get("accountNumber")), emptyToNull(row.get("content")),
                        emptyToNull(row.get("sampleReason")), emptyToNull(row.get("auditResult")), emptyToNull(row.get("recommendationType")),
                        emptyToNull(row.get("transactionStaff")), emptyToNull(row.get("controlUser")), emptyToNull(row.get("controlStaff")),
                        emptyToNull(row.get("controlStaffTitle")), existing.map(AuditCmNtd12::isActive).orElse(true));
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

        auditLogService.record("AuditCmNtd12", null, AuditAction.CREATE,
                "Import Excel but toan chon mau TCKT: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditCmNtd12 item, AuditCmNtd12Request request) {
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
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, BigDecimal entryNumber, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndEntryNumber(tenantId, branchCode, transactionDate, postingUser, entryNumber)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_NTD12_DUPLICATE", "Ban ghi nay da ton tai: " + entryNumber);
                });
    }

    private AuditCmNtd12 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_NTD12_NOT_FOUND", "Khong tim thay ban ghi but toan chon mau", HttpStatus.NOT_FOUND));
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
                new ExportColumn("controlStaffTitle", "Chức danh người kiểm soát"));
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

    private AuditCmNtd12Response toResponse(AuditCmNtd12 item) {
        return new AuditCmNtd12Response(item.getId(), item.getBranchCode(), item.getTransactionDate(), item.getPostingUser(),
                item.getEntryNumber(), item.getDebitAmount(), item.getCreditAmount(), item.getTransactionStatus(), item.getCurrency(),
                item.getAccountNumber(), item.getContent(), item.getSampleReason(), item.getAuditResult(), item.getRecommendationType(),
                item.getTransactionStaff(), item.getControlUser(), item.getControlStaff(), item.getControlStaffTitle(), item.isActive());
    }
}
