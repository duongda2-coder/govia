package com.govia.audit.cmntd13.service;

import com.govia.audit.cmntd13.dto.AuditCmNtd13Request;
import com.govia.audit.cmntd13.dto.AuditCmNtd13Response;
import com.govia.audit.cmntd13.entity.AuditCmNtd13;
import com.govia.audit.cmntd13.repository.AuditCmNtd13Repository;
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

/** CRUD + Import/Export cho "Kết quả kiểm toán đơn vị chấp nhận thẻ (07D)" (sheet ZTC_CM_NTD13). */
@Service
public class AuditCmNtd13Service {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
    };

    private final AuditCmNtd13Repository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmNtd13Service(AuditCmNtd13Repository repository, AuditLogService auditLogService,
                                ExcelExportService excelExportService, WordExportService wordExportService,
                                ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditCmNtd13Response> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditCmNtd13Response create(AuditCmNtd13Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.occurrenceDate(), request.merchantAccountNumber(),
                request.businessRegistrationName(), null);

        AuditCmNtd13 item = new AuditCmNtd13();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd13", item.getId(), AuditAction.CREATE,
                "Tao ban ghi chon mau DVCNT: " + item.getBranchCode() + " - " + item.getBusinessRegistrationName());
        return toResponse(item);
    }

    @Transactional
    public AuditCmNtd13Response update(UUID id, AuditCmNtd13Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd13 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.occurrenceDate(), request.merchantAccountNumber(),
                request.businessRegistrationName(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd13", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi chon mau DVCNT: " + item.getBranchCode() + " - " + item.getBusinessRegistrationName());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd13 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmNtd13", id, AuditAction.DELETE,
                "Xoa ban ghi chon mau DVCNT: " + item.getBranchCode() + " - " + item.getBusinessRegistrationName());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_cm_ntd13", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Kết quả kiểm toán đơn vị chấp nhận thẻ (07D)", exportColumns(), exportRows());
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
                String merchantAccountNumber = row.get("merchantAccountNumber");
                String businessRegistrationName = row.get("businessRegistrationName");
                LocalDate occurrenceDate = parseDate(row.get("occurrenceDate"));
                if (isBlank(branchCode) || isBlank(merchantAccountNumber) || isBlank(businessRegistrationName) || occurrenceDate == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED",
                            "Thieu Ma chi nhanh, Ngay phat sinh, So tai khoan DVCNT hoac Ten DKKD");
                }

                Optional<AuditCmNtd13> existing = repository
                        .findByTenantIdAndBranchCodeAndOccurrenceDateAndMerchantAccountNumberAndBusinessRegistrationName(
                                tenantId, branchCode.trim(), occurrenceDate, merchantAccountNumber.trim(), businessRegistrationName.trim());
                AuditCmNtd13Request request = new AuditCmNtd13Request(branchCode.trim(), occurrenceDate,
                        emptyToNull(row.get("merchantId")), merchantAccountNumber.trim(), businessRegistrationName.trim(),
                        emptyToNull(row.get("status")), emptyToNull(row.get("sampleReason")), emptyToNull(row.get("auditResult")),
                        emptyToNull(row.get("recommendationType")), emptyToNull(row.get("transactionStaff")),
                        emptyToNull(row.get("controlUser")), emptyToNull(row.get("controlStaff")), emptyToNull(row.get("controlStaffTitle")),
                        existing.map(AuditCmNtd13::isActive).orElse(true));
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

        auditLogService.record("AuditCmNtd13", null, AuditAction.CREATE,
                "Import Excel chon mau DVCNT: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditCmNtd13 item, AuditCmNtd13Request request) {
        item.setBranchCode(request.branchCode());
        item.setOccurrenceDate(request.occurrenceDate());
        item.setMerchantId(request.merchantId());
        item.setMerchantAccountNumber(request.merchantAccountNumber());
        item.setBusinessRegistrationName(request.businessRegistrationName());
        item.setStatus(request.status());
        item.setSampleReason(request.sampleReason());
        item.setAuditResult(request.auditResult());
        item.setRecommendationType(request.recommendationType());
        item.setTransactionStaff(request.transactionStaff());
        item.setControlUser(request.controlUser());
        item.setControlStaff(request.controlStaff());
        item.setControlStaffTitle(request.controlStaffTitle());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, LocalDate occurrenceDate, String merchantAccountNumber,
                                   String businessRegistrationName, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndOccurrenceDateAndMerchantAccountNumberAndBusinessRegistrationName(
                        tenantId, branchCode, occurrenceDate, merchantAccountNumber, businessRegistrationName)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_NTD13_DUPLICATE", "Ban ghi nay da ton tai: " + businessRegistrationName);
                });
    }

    private AuditCmNtd13 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_NTD13_NOT_FOUND", "Khong tim thay ban ghi chon mau DVCNT", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("occurrenceDate", "Ngày phát sinh"),
                new ExportColumn("merchantId", "Mechant ID"),
                new ExportColumn("merchantAccountNumber", "Số tài khoản ĐVCNT"),
                new ExportColumn("businessRegistrationName", "Tên ĐKKD"),
                new ExportColumn("status", "Trạng thái"),
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
                    row.put("occurrenceDate", item.getOccurrenceDate() == null ? null : item.getOccurrenceDate().format(DATE_FORMATS[0]));
                    row.put("merchantId", item.getMerchantId());
                    row.put("merchantAccountNumber", item.getMerchantAccountNumber());
                    row.put("businessRegistrationName", item.getBusinessRegistrationName());
                    row.put("status", item.getStatus());
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

    private AuditCmNtd13Response toResponse(AuditCmNtd13 item) {
        return new AuditCmNtd13Response(item.getId(), item.getBranchCode(), item.getOccurrenceDate(), item.getMerchantId(),
                item.getMerchantAccountNumber(), item.getBusinessRegistrationName(), item.getStatus(), item.getSampleReason(),
                item.getAuditResult(), item.getRecommendationType(), item.getTransactionStaff(), item.getControlUser(),
                item.getControlStaff(), item.getControlStaffTitle(), item.isActive());
    }
}
