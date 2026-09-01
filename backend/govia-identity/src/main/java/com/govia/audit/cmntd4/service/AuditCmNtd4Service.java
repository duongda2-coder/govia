package com.govia.audit.cmntd4.service;

import com.govia.audit.cmntd4.dto.AuditCmNtd4Request;
import com.govia.audit.cmntd4.dto.AuditCmNtd4Response;
import com.govia.audit.cmntd4.entity.AuditCmNtd4;
import com.govia.audit.cmntd4.repository.AuditCmNtd4Repository;
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

/** CRUD + Import/Export cho "Danh sach chon mau LC va nho thu TTQT" (sheet ZTC_CM_NTD4). */
@Service
public class AuditCmNtd4Service {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
    };

    private final AuditCmNtd4Repository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmNtd4Service(AuditCmNtd4Repository repository, AuditLogService auditLogService,
                               ExcelExportService excelExportService, WordExportService wordExportService,
                               ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditCmNtd4Response> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditCmNtd4Response create(AuditCmNtd4Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.referenceNumber(), request.openDate(), request.customerName(), null);

        AuditCmNtd4 item = new AuditCmNtd4();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd4", item.getId(), AuditAction.CREATE,
                "Tao ban ghi chon mau LC va nho thu TTQT: " + item.getBranchCode() + " - " + item.getCustomerName());
        return toResponse(item);
    }

    @Transactional
    public AuditCmNtd4Response update(UUID id, AuditCmNtd4Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd4 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.referenceNumber(), request.openDate(), request.customerName(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd4", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi chon mau LC va nho thu TTQT: " + item.getBranchCode() + " - " + item.getCustomerName());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd4 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmNtd4", id, AuditAction.DELETE,
                "Xoa ban ghi chon mau LC va nho thu TTQT: " + item.getBranchCode() + " - " + item.getCustomerName());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_cm_ntd4", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Danh sách chọn mẫu LC và nhờ thu TTQT", exportColumns(), exportRows());
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
                BigDecimal referenceNumber = parseDecimal(row.get("referenceNumber"));
                LocalDate openDate = parseDate(row.get("openDate"));
                if (isBlank(branchCode) || isBlank(customerName) || referenceNumber == null || openDate == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh, So tham chieu, Ngay mo hoac Ten khach hang");
                }

                Optional<AuditCmNtd4> existing = repository.findByTenantIdAndBranchCodeAndReferenceNumberAndOpenDateAndCustomerName(
                        tenantId, branchCode.trim(), referenceNumber, openDate, customerName.trim());
                AuditCmNtd4Request request = new AuditCmNtd4Request(branchCode.trim(), referenceNumber, openDate,
                        emptyToNull(row.get("customerCode")), customerName.trim(), parseDecimal(row.get("amount")),
                        emptyToNull(row.get("beneficiary")), emptyToNull(row.get("auditResult")), emptyToNull(row.get("recommendationType")),
                        emptyToNull(row.get("transactionStaff")), emptyToNull(row.get("controlUser")), emptyToNull(row.get("controlStaff")),
                        emptyToNull(row.get("controlStaffTitle")), existing.map(AuditCmNtd4::isActive).orElse(true));
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

        auditLogService.record("AuditCmNtd4", null, AuditAction.CREATE,
                "Import Excel chon mau LC va nho thu TTQT: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditCmNtd4 item, AuditCmNtd4Request request) {
        item.setBranchCode(request.branchCode());
        item.setReferenceNumber(request.referenceNumber());
        item.setOpenDate(request.openDate());
        item.setCustomerCode(request.customerCode());
        item.setCustomerName(request.customerName());
        item.setAmount(request.amount());
        item.setBeneficiary(request.beneficiary());
        item.setAuditResult(request.auditResult());
        item.setRecommendationType(request.recommendationType());
        item.setTransactionStaff(request.transactionStaff());
        item.setControlUser(request.controlUser());
        item.setControlStaff(request.controlStaff());
        item.setControlStaffTitle(request.controlStaffTitle());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, BigDecimal referenceNumber, LocalDate openDate, String customerName, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndReferenceNumberAndOpenDateAndCustomerName(tenantId, branchCode, referenceNumber, openDate, customerName)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_NTD4_DUPLICATE", "Ban ghi nay da ton tai: " + customerName);
                });
    }

    private AuditCmNtd4 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_NTD4_NOT_FOUND", "Khong tim thay ban ghi chon mau LC va nho thu TTQT", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("referenceNumber", "Số tham chiếu"),
                new ExportColumn("openDate", "Ngày mở"),
                new ExportColumn("customerCode", "Mã Khách hàng"),
                new ExportColumn("customerName", "Tên khách hàng"),
                new ExportColumn("amount", "Số tiền"),
                new ExportColumn("beneficiary", "Người thụ hưởng"),
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
                    row.put("referenceNumber", item.getReferenceNumber());
                    row.put("openDate", item.getOpenDate() == null ? null : item.getOpenDate().format(DATE_FORMATS[0]));
                    row.put("customerCode", item.getCustomerCode());
                    row.put("customerName", item.getCustomerName());
                    row.put("amount", item.getAmount());
                    row.put("beneficiary", item.getBeneficiary());
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

    private AuditCmNtd4Response toResponse(AuditCmNtd4 item) {
        return new AuditCmNtd4Response(item.getId(), item.getBranchCode(), item.getReferenceNumber(), item.getOpenDate(),
                item.getCustomerCode(), item.getCustomerName(), item.getAmount(), item.getBeneficiary(), item.getAuditResult(),
                item.getRecommendationType(), item.getTransactionStaff(), item.getControlUser(), item.getControlStaff(),
                item.getControlStaffTitle(), item.isActive());
    }
}
