package com.govia.audit.cmntd9.service;

import com.govia.audit.cmntd9.dto.AuditCmNtd9Request;
import com.govia.audit.cmntd9.dto.AuditCmNtd9Response;
import com.govia.audit.cmntd9.entity.AuditCmNtd9;
import com.govia.audit.cmntd9.repository.AuditCmNtd9Repository;
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

/** CRUD + Import/Export cho "Danh sach chon mau khach hang lan dau su dung dich vu cua NH" (sheet ZTC_CM_NTD9). */
@Service
public class AuditCmNtd9Service {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
    };

    private final AuditCmNtd9Repository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmNtd9Service(AuditCmNtd9Repository repository, AuditLogService auditLogService,
                               ExcelExportService excelExportService, WordExportService wordExportService,
                               ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditCmNtd9Response> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditCmNtd9Response create(AuditCmNtd9Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.transactionDate(), request.postingUser(), request.customerName(), null);

        AuditCmNtd9 item = new AuditCmNtd9();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd9", item.getId(), AuditAction.CREATE,
                "Tao ban ghi chon mau KH lan dau: " + item.getBranchCode() + " - " + item.getCustomerName());
        return toResponse(item);
    }

    @Transactional
    public AuditCmNtd9Response update(UUID id, AuditCmNtd9Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd9 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.transactionDate(), request.postingUser(), request.customerName(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd9", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi chon mau KH lan dau: " + item.getBranchCode() + " - " + item.getCustomerName());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd9 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmNtd9", id, AuditAction.DELETE,
                "Xoa ban ghi chon mau KH lan dau: " + item.getBranchCode() + " - " + item.getCustomerName());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_cm_ntd9", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Danh sách chọn mẫu khách hàng lần đầu sử dụng dịch vụ của NH", exportColumns(), exportRows());
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
                String customerName = row.get("customerName");
                LocalDate transactionDate = parseDate(row.get("transactionDate"));
                if (isBlank(branchCode) || isBlank(postingUser) || isBlank(customerName) || transactionDate == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh, User hach toan, Ten khach hang hoac Ngay giao dich thuc te");
                }

                Optional<AuditCmNtd9> existing = repository.findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndCustomerName(
                        tenantId, branchCode.trim(), transactionDate, postingUser.trim(), customerName.trim());
                AuditCmNtd9Request request = new AuditCmNtd9Request(branchCode.trim(), transactionDate, postingUser.trim(),
                        emptyToNull(row.get("customerCode")), customerName.trim(), emptyToNull(row.get("idNumber")),
                        emptyToNull(row.get("customerType")), emptyToNull(row.get("transactionContent")), emptyToNull(row.get("sampleReason")),
                        emptyToNull(row.get("auditResult")), emptyToNull(row.get("recommendationType")), emptyToNull(row.get("transactionStaff")),
                        emptyToNull(row.get("controlUser")), emptyToNull(row.get("controlStaff")), emptyToNull(row.get("controlStaffTitle")),
                        existing.map(AuditCmNtd9::isActive).orElse(true));
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

        auditLogService.record("AuditCmNtd9", null, AuditAction.CREATE,
                "Import Excel chon mau KH lan dau: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditCmNtd9 item, AuditCmNtd9Request request) {
        item.setBranchCode(request.branchCode());
        item.setTransactionDate(request.transactionDate());
        item.setPostingUser(request.postingUser());
        item.setCustomerCode(request.customerCode());
        item.setCustomerName(request.customerName());
        item.setIdNumber(request.idNumber());
        item.setCustomerType(request.customerType());
        item.setTransactionContent(request.transactionContent());
        item.setSampleReason(request.sampleReason());
        item.setAuditResult(request.auditResult());
        item.setRecommendationType(request.recommendationType());
        item.setTransactionStaff(request.transactionStaff());
        item.setControlUser(request.controlUser());
        item.setControlStaff(request.controlStaff());
        item.setControlStaffTitle(request.controlStaffTitle());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, String customerName, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndCustomerName(tenantId, branchCode, transactionDate, postingUser, customerName)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_NTD9_DUPLICATE", "Ban ghi nay da ton tai: " + customerName);
                });
    }

    private AuditCmNtd9 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_NTD9_NOT_FOUND", "Khong tim thay ban ghi chon mau", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("transactionDate", "Ngày giao dịch thực tế"),
                new ExportColumn("postingUser", "User hạch toán"),
                new ExportColumn("customerCode", "Mã Khách hàng"),
                new ExportColumn("customerName", "Tên khách hàng"),
                new ExportColumn("idNumber", "Số CMND/Hộ chiếu"),
                new ExportColumn("customerType", "Loại KH"),
                new ExportColumn("transactionContent", "Nội dung giao dịch"),
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
                    row.put("customerCode", item.getCustomerCode());
                    row.put("customerName", item.getCustomerName());
                    row.put("idNumber", item.getIdNumber());
                    row.put("customerType", item.getCustomerType());
                    row.put("transactionContent", item.getTransactionContent());
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

    private AuditCmNtd9Response toResponse(AuditCmNtd9 item) {
        return new AuditCmNtd9Response(item.getId(), item.getBranchCode(), item.getTransactionDate(), item.getPostingUser(),
                item.getCustomerCode(), item.getCustomerName(), item.getIdNumber(), item.getCustomerType(), item.getTransactionContent(),
                item.getSampleReason(), item.getAuditResult(), item.getRecommendationType(), item.getTransactionStaff(),
                item.getControlUser(), item.getControlStaff(), item.getControlStaffTitle(), item.isActive());
    }
}
