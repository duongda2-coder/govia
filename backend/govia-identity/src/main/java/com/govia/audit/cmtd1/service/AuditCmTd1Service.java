package com.govia.audit.cmtd1.service;

import com.govia.audit.cmtd1.dto.AuditCmTd1Request;
import com.govia.audit.cmtd1.dto.AuditCmTd1Response;
import com.govia.audit.cmtd1.entity.AuditCmTd1;
import com.govia.audit.cmtd1.repository.AuditCmTd1Repository;
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

/** CRUD + Import/Export cho "Danh sach khach hang chon mau tin dung" (sheet ZTC_CM_TD1). */
@Service
public class AuditCmTd1Service {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
    };

    private final AuditCmTd1Repository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmTd1Service(AuditCmTd1Repository repository, AuditLogService auditLogService,
                              ExcelExportService excelExportService, WordExportService wordExportService,
                              ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditCmTd1Response> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditCmTd1Response create(AuditCmTd1Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.auditDate(), request.customerName(), null);

        AuditCmTd1 item = new AuditCmTd1();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmTd1", item.getId(), AuditAction.CREATE,
                "Tao ban ghi chon mau tin dung: " + item.getBranchCode() + " - " + item.getCustomerName());
        return toResponse(item);
    }

    @Transactional
    public AuditCmTd1Response update(UUID id, AuditCmTd1Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmTd1 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.auditDate(), request.customerName(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmTd1", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi chon mau tin dung: " + item.getBranchCode() + " - " + item.getCustomerName());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmTd1 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmTd1", id, AuditAction.DELETE,
                "Xoa ban ghi chon mau tin dung: " + item.getBranchCode() + " - " + item.getCustomerName());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_cm_td1", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Danh sách khách hàng chọn mẫu tín dụng", exportColumns(), exportRows());
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
                LocalDate auditDate = parseDate(row.get("auditDate"));
                if (isBlank(branchCode) || isBlank(customerName) || auditDate == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh, Ten khach hang hoac Thoi diem kiem toan");
                }

                Optional<AuditCmTd1> existing =
                        repository.findByTenantIdAndBranchCodeAndAuditDateAndCustomerName(tenantId, branchCode.trim(), auditDate, customerName.trim());
                AuditCmTd1Request request = new AuditCmTd1Request(branchCode.trim(), auditDate, emptyToNull(row.get("customerCode")),
                        customerName.trim(), parseDecimal(row.get("approvedAmount")), emptyToNull(row.get("loanPurpose")),
                        emptyToNull(row.get("description")), parseDecimal(row.get("onBalanceDebt")), parseDecimal(row.get("guaranteeBalance")),
                        parseDecimal(row.get("riskClassifiedDebt")), parseDecimal(row.get("vamcSoldDebt")), emptyToNull(row.get("debtGroup")),
                        emptyToNull(row.get("auditScope")), emptyToNull(row.get("auditorCode")), emptyToNull(row.get("sampleReason")),
                        emptyToNull(row.get("note")), existing.map(AuditCmTd1::isActive).orElse(true));
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

        auditLogService.record("AuditCmTd1", null, AuditAction.CREATE,
                "Import Excel chon mau tin dung: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditCmTd1 item, AuditCmTd1Request request) {
        item.setBranchCode(request.branchCode());
        item.setAuditDate(request.auditDate());
        item.setCustomerCode(request.customerCode());
        item.setCustomerName(request.customerName());
        item.setApprovedAmount(request.approvedAmount());
        item.setLoanPurpose(request.loanPurpose());
        item.setDescription(request.description());
        item.setOnBalanceDebt(request.onBalanceDebt());
        item.setGuaranteeBalance(request.guaranteeBalance());
        item.setRiskClassifiedDebt(request.riskClassifiedDebt());
        item.setVamcSoldDebt(request.vamcSoldDebt());
        item.setTotalCreditBalance(sum(request.onBalanceDebt(), request.guaranteeBalance(), request.riskClassifiedDebt(), request.vamcSoldDebt()));
        item.setDebtGroup(request.debtGroup());
        item.setAuditScope(request.auditScope());
        item.setAuditorCode(request.auditorCode());
        item.setSampleReason(request.sampleReason());
        item.setNote(request.note());
        item.setActive(request.active());
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            if (v != null) {
                total = total.add(v);
            }
        }
        return total;
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, LocalDate auditDate, String customerName, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndAuditDateAndCustomerName(tenantId, branchCode, auditDate, customerName)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_TD1_DUPLICATE", "Ban ghi nay da ton tai: " + customerName);
                });
    }

    private AuditCmTd1 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_TD1_NOT_FOUND", "Khong tim thay ban ghi chon mau tin dung", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("auditDate", "Thời điểm kiểm toán"),
                new ExportColumn("customerCode", "Mã Khách hàng"),
                new ExportColumn("customerName", "Tên khách hàng"),
                new ExportColumn("approvedAmount", "Số tiền phê duyệt cho vay/ cấp bảo lãnh"),
                new ExportColumn("loanPurpose", "Mục đích vay vốn"),
                new ExportColumn("description", "Diễn giải"),
                new ExportColumn("onBalanceDebt", "Dư nợ nội bảng"),
                new ExportColumn("guaranteeBalance", "Số dư bảo lãnh/LC"),
                new ExportColumn("riskClassifiedDebt", "Dư nợ XLRR"),
                new ExportColumn("vamcSoldDebt", "Dư nợ bán cho VAMC"),
                new ExportColumn("totalCreditBalance", "Tổng số dư cấp tín dụng"),
                new ExportColumn("debtGroup", "Nhóm nợ"),
                new ExportColumn("auditScope", "Phạm vi kiểm toán"),
                new ExportColumn("auditorCode", "Cán bộ kiểm toán"),
                new ExportColumn("sampleReason", "Lý do chọn mẫu"),
                new ExportColumn("note", "Ghi chú"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("branchCode", item.getBranchCode());
                    row.put("auditDate", item.getAuditDate() == null ? null : item.getAuditDate().format(DATE_FORMATS[0]));
                    row.put("customerCode", item.getCustomerCode());
                    row.put("customerName", item.getCustomerName());
                    row.put("approvedAmount", item.getApprovedAmount());
                    row.put("loanPurpose", item.getLoanPurpose());
                    row.put("description", item.getDescription());
                    row.put("onBalanceDebt", item.getOnBalanceDebt());
                    row.put("guaranteeBalance", item.getGuaranteeBalance());
                    row.put("riskClassifiedDebt", item.getRiskClassifiedDebt());
                    row.put("vamcSoldDebt", item.getVamcSoldDebt());
                    row.put("totalCreditBalance", item.getTotalCreditBalance());
                    row.put("debtGroup", item.getDebtGroup());
                    row.put("auditScope", item.getAuditScope());
                    row.put("auditorCode", item.getAuditorCode());
                    row.put("sampleReason", item.getSampleReason());
                    row.put("note", item.getNote());
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

    private AuditCmTd1Response toResponse(AuditCmTd1 item) {
        return new AuditCmTd1Response(item.getId(), item.getBranchCode(), item.getAuditDate(), item.getCustomerCode(),
                item.getCustomerName(), item.getApprovedAmount(), item.getLoanPurpose(), item.getDescription(),
                item.getOnBalanceDebt(), item.getGuaranteeBalance(), item.getRiskClassifiedDebt(), item.getVamcSoldDebt(),
                item.getTotalCreditBalance(), item.getDebtGroup(), item.getAuditScope(), item.getAuditorCode(),
                item.getSampleReason(), item.getNote(), item.isActive());
    }
}
