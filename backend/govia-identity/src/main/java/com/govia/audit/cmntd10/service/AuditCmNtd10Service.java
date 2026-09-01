package com.govia.audit.cmntd10.service;

import com.govia.audit.cmntd10.dto.AuditCmNtd10Request;
import com.govia.audit.cmntd10.dto.AuditCmNtd10Response;
import com.govia.audit.cmntd10.entity.AuditCmNtd10;
import com.govia.audit.cmntd10.repository.AuditCmNtd10Repository;
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

/** CRUD + Import/Export cho "Ket qua kiem toan ho so phat hanh the" (sheet ZTC_CM_NTD10). */
@Service
public class AuditCmNtd10Service {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
    };

    private final AuditCmNtd10Repository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmNtd10Service(AuditCmNtd10Repository repository, AuditLogService auditLogService,
                                ExcelExportService excelExportService, WordExportService wordExportService,
                                ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditCmNtd10Response> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditCmNtd10Response create(AuditCmNtd10Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.issueDate(), request.customerName(), request.accountNumber(), null);

        AuditCmNtd10 item = new AuditCmNtd10();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd10", item.getId(), AuditAction.CREATE,
                "Tao ban ghi kiem toan phat hanh the: " + item.getBranchCode() + " - " + item.getCustomerName());
        return toResponse(item);
    }

    @Transactional
    public AuditCmNtd10Response update(UUID id, AuditCmNtd10Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd10 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.issueDate(), request.customerName(), request.accountNumber(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd10", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi kiem toan phat hanh the: " + item.getBranchCode() + " - " + item.getCustomerName());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd10 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmNtd10", id, AuditAction.DELETE,
                "Xoa ban ghi kiem toan phat hanh the: " + item.getBranchCode() + " - " + item.getCustomerName());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_cm_ntd10", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Kết quả kiểm toán hồ sơ phát hành thẻ", exportColumns(), exportRows());
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
                String customerName = row.get("customerName");
                String accountNumber = row.get("accountNumber");
                LocalDate issueDate = parseDate(row.get("issueDate"));
                if (isBlank(branchCode) || isBlank(customerName) || isBlank(accountNumber) || issueDate == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh, Ten khach hang, So tai khoan hoac Ngay phat hanh");
                }

                Optional<AuditCmNtd10> existing = repository.findByTenantIdAndBranchCodeAndIssueDateAndCustomerNameAndAccountNumber(
                        tenantId, branchCode.trim(), issueDate, customerName.trim(), accountNumber.trim());
                AuditCmNtd10Request request = new AuditCmNtd10Request(branchCode.trim(), issueDate, emptyToNull(row.get("customerCode")),
                        customerName.trim(), accountNumber.trim(), emptyToNull(row.get("cardTier")), emptyToNull(row.get("issuingUser")),
                        parseDecimal(row.get("issuanceFee")), emptyToNull(row.get("issuanceType")), emptyToNull(row.get("issuanceOccurrence")),
                        emptyToNull(row.get("sampleReason")), emptyToNull(row.get("auditResult")), emptyToNull(row.get("recommendationType")),
                        emptyToNull(row.get("transactionStaff")), emptyToNull(row.get("controlUser")), emptyToNull(row.get("controlStaff")),
                        emptyToNull(row.get("controlStaffTitle")), existing.map(AuditCmNtd10::isActive).orElse(true));
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

        auditLogService.record("AuditCmNtd10", null, AuditAction.CREATE,
                "Import Excel kiem toan phat hanh the: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditCmNtd10 item, AuditCmNtd10Request request) {
        item.setBranchCode(request.branchCode());
        item.setIssueDate(request.issueDate());
        item.setCustomerCode(request.customerCode());
        item.setCustomerName(request.customerName());
        item.setAccountNumber(request.accountNumber());
        item.setCardTier(request.cardTier());
        item.setIssuingUser(request.issuingUser());
        item.setIssuanceFee(request.issuanceFee());
        item.setIssuanceType(request.issuanceType());
        item.setIssuanceOccurrence(request.issuanceOccurrence());
        item.setSampleReason(request.sampleReason());
        item.setAuditResult(request.auditResult());
        item.setRecommendationType(request.recommendationType());
        item.setTransactionStaff(request.transactionStaff());
        item.setControlUser(request.controlUser());
        item.setControlStaff(request.controlStaff());
        item.setControlStaffTitle(request.controlStaffTitle());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, LocalDate issueDate, String customerName, String accountNumber, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndIssueDateAndCustomerNameAndAccountNumber(tenantId, branchCode, issueDate, customerName, accountNumber)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_NTD10_DUPLICATE", "Ban ghi nay da ton tai: " + customerName);
                });
    }

    private AuditCmNtd10 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_NTD10_NOT_FOUND", "Khong tim thay ban ghi kiem toan phat hanh the", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("issueDate", "Ngày phát hành"),
                new ExportColumn("customerCode", "Mã Khách hàng"),
                new ExportColumn("customerName", "Tên khách hàng"),
                new ExportColumn("accountNumber", "Số tài khoản"),
                new ExportColumn("cardTier", "Hạng thẻ"),
                new ExportColumn("issuingUser", "User phát hành"),
                new ExportColumn("issuanceFee", "Phí phát hành, phát hành lại"),
                new ExportColumn("issuanceType", "Loại phát hành"),
                new ExportColumn("issuanceOccurrence", "Lần phát hành"),
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
                    row.put("issueDate", item.getIssueDate() == null ? null : item.getIssueDate().format(DATE_FORMATS[0]));
                    row.put("customerCode", item.getCustomerCode());
                    row.put("customerName", item.getCustomerName());
                    row.put("accountNumber", item.getAccountNumber());
                    row.put("cardTier", item.getCardTier());
                    row.put("issuingUser", item.getIssuingUser());
                    row.put("issuanceFee", item.getIssuanceFee());
                    row.put("issuanceType", item.getIssuanceType());
                    row.put("issuanceOccurrence", item.getIssuanceOccurrence());
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

    private AuditCmNtd10Response toResponse(AuditCmNtd10 item) {
        return new AuditCmNtd10Response(item.getId(), item.getBranchCode(), item.getIssueDate(), item.getCustomerCode(),
                item.getCustomerName(), item.getAccountNumber(), item.getCardTier(), item.getIssuingUser(), item.getIssuanceFee(),
                item.getIssuanceType(), item.getIssuanceOccurrence(), item.getSampleReason(), item.getAuditResult(),
                item.getRecommendationType(), item.getTransactionStaff(), item.getControlUser(), item.getControlStaff(),
                item.getControlStaffTitle(), item.isActive());
    }
}
