package com.govia.audit.cmtd2.service;

import com.govia.audit.cmtd2.dto.AuditCmTd2Request;
import com.govia.audit.cmtd2.dto.AuditCmTd2Response;
import com.govia.audit.cmtd2.entity.AuditCmTd2;
import com.govia.audit.cmtd2.repository.AuditCmTd2Repository;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.processstep.entity.AuditProcessStepSummary;
import com.govia.audit.processstep.repository.AuditProcessStepSummaryRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
import com.govia.core.export.WordExportService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Employee;
import com.govia.identity.entity.UserAccount;
import com.govia.identity.repository.EmployeeRepository;
import com.govia.identity.repository.UserAccountRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** CRUD + Import/Export cho "Ket qua kiem toan chon mau cac but toan giao dich huy, lui ngay"
 * (sheet ZTC_CM_TD2). */
@Service
public class AuditCmTd2Service {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
    };

    private final AuditCmTd2Repository repository;
    private final AuditEngagementRepository engagementRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditProcessStepSummaryRepository processStepSummaryRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmTd2Service(AuditCmTd2Repository repository, AuditEngagementRepository engagementRepository,
                              EmployeeRepository employeeRepository, UserAccountRepository userAccountRepository,
                              AuditProcessStepSummaryRepository processStepSummaryRepository, AuditLogService auditLogService,
                              ExcelExportService excelExportService, WordExportService wordExportService,
                              ExcelImportService excelImportService) {
        this.repository = repository;
        this.engagementRepository = engagementRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.processStepSummaryRepository = processStepSummaryRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    /** Danh sach LUON loc theo 1 Cuoc kiem toan cu the (giong bo loc "Nam" ben Cham diem rui ro) -
     * khong tra ve "tat ca" khi thieu engagementId. */
    @Transactional(readOnly = true)
    public List<AuditCmTd2Response> list(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        ResponseContext ctx = buildResponseContext(tenantId);
        return repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId).stream()
                .map(item -> toResponse(item, ctx)).toList();
    }

    @Transactional
    public AuditCmTd2Response create(AuditCmTd2Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.transactionDate(), request.postingUser(), request.customerName(), null);
        validateEngagement(tenantId, request.engagementId());

        AuditCmTd2 item = new AuditCmTd2();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmTd2", item.getId(), AuditAction.CREATE,
                "Tao ban ghi chon mau but toan huy/lui ngay: " + item.getBranchCode() + " - " + item.getCustomerName());
        return toResponse(item, buildResponseContext(tenantId));
    }

    @Transactional
    public AuditCmTd2Response update(UUID id, AuditCmTd2Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmTd2 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.transactionDate(), request.postingUser(), request.customerName(), id);
        validateEngagement(tenantId, request.engagementId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmTd2", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi chon mau but toan huy/lui ngay: " + item.getBranchCode() + " - " + item.getCustomerName());
        return toResponse(item, buildResponseContext(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmTd2 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmTd2", id, AuditAction.DELETE,
                "Xoa ban ghi chon mau but toan huy/lui ngay: " + item.getBranchCode() + " - " + item.getCustomerName());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(UUID engagementId) {
        return excelExportService.export("audit_cm_td2", exportColumns(), exportRows(engagementId));
    }

    @Transactional(readOnly = true)
    public byte[] exportWord(UUID engagementId) {
        return wordExportService.export("Kết quả kiểm toán chọn mẫu các bút toán giao dịch hủy, lùi ngày", exportColumns(), exportRows(engagementId));
    }

    /** Import luon gan vao 1 Cuoc kiem toan cu the (dot dang duoc loc tren man hinh khi bam Import). */
    @Transactional
    public ImportResult importFromExcel(UUID engagementId, MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), exportColumns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }

        UUID tenantId = TenantContext.getTenantId();
        validateEngagement(tenantId, engagementId);
        Map<String, UUID> employeeIdsByUsername = userAccountRepository.findByTenantId(tenantId).stream()
                .filter(a -> a.getEmployeeId() != null)
                .collect(Collectors.toMap(UserAccount::getUsername, UserAccount::getEmployeeId, (a, b) -> a));
        Map<String, UUID> stepSummaryIdsByCode = processStepSummaryRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditProcessStepSummary::getCode, AuditProcessStepSummary::getId, (a, b) -> a));

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
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh, User hach toan, Ten KH hoac Ngay giao dich thuc te");
                }
                String assignedUsername = row.get("assignedUsername");
                String stepSummaryCode = row.get("processStepSummaryCode");

                Optional<AuditCmTd2> existing = repository.findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndCustomerName(
                        tenantId, branchCode.trim(), transactionDate, postingUser.trim(), customerName.trim());
                AuditCmTd2Request request = new AuditCmTd2Request(engagementId,
                        isBlank(assignedUsername) ? null : employeeIdsByUsername.get(assignedUsername.trim()),
                        isBlank(stepSummaryCode) ? null : stepSummaryIdsByCode.get(stepSummaryCode.trim()),
                        branchCode.trim(), transactionDate, parseDate(row.get("valueDate")),
                        postingUser.trim(), parseDecimal(row.get("entryNumber")), emptyToNull(row.get("customerCode")), customerName.trim(),
                        emptyToNull(row.get("disbursementNumber")), emptyToNull(row.get("businessCode")), emptyToNull(row.get("transactionStatus")),
                        emptyToNull(row.get("currency")), parseDecimal(row.get("debitAmount")), parseDecimal(row.get("creditAmount")),
                        emptyToNull(row.get("accountNumber")), emptyToNull(row.get("ipcasReviewResult")), emptyToNull(row.get("documentCheckResult")),
                        existing.map(AuditCmTd2::isActive).orElse(true));
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

        auditLogService.record("AuditCmTd2", null, AuditAction.CREATE,
                "Import Excel chon mau but toan huy/lui ngay: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditCmTd2 item, AuditCmTd2Request request) {
        item.setEngagementId(request.engagementId());
        item.setAssignedEmployeeId(request.assignedEmployeeId());
        item.setProcessStepSummaryId(request.processStepSummaryId());
        item.setBranchCode(request.branchCode());
        item.setTransactionDate(request.transactionDate());
        item.setValueDate(request.valueDate());
        item.setPostingUser(request.postingUser());
        item.setEntryNumber(request.entryNumber());
        item.setCustomerCode(request.customerCode());
        item.setCustomerName(request.customerName());
        item.setDisbursementNumber(request.disbursementNumber());
        item.setBusinessCode(request.businessCode());
        item.setTransactionStatus(request.transactionStatus());
        item.setCurrency(request.currency());
        item.setDebitAmount(request.debitAmount());
        item.setCreditAmount(request.creditAmount());
        item.setAccountNumber(request.accountNumber());
        item.setPostingDateDiff(computeDateDiff(request.transactionDate(), request.valueDate()));
        item.setIpcasReviewResult(request.ipcasReviewResult());
        item.setDocumentCheckResult(request.documentCheckResult());
        item.setActive(request.active());
    }

    private BigDecimal computeDateDiff(LocalDate transactionDate, LocalDate valueDate) {
        if (transactionDate == null || valueDate == null) {
            return null;
        }
        return BigDecimal.valueOf(ChronoUnit.DAYS.between(transactionDate, valueDate));
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, String customerName, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndCustomerName(tenantId, branchCode, transactionDate, postingUser, customerName)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_TD2_DUPLICATE", "Ban ghi nay da ton tai: " + customerName);
                });
    }

    private AuditCmTd2 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_TD2_NOT_FOUND", "Khong tim thay ban ghi chon mau but toan huy/lui ngay", HttpStatus.NOT_FOUND));
    }

    private void validateEngagement(UUID tenantId, UUID engagementId) {
        engagementRepository.findById(engagementId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_ENGAGEMENT_NOT_FOUND", "Khong tim thay cuoc kiem toan"));
    }

    /** Gom du lieu lien quan (Cuoc kiem toan, nhan vien duoc phan cong, Buoc quy trinh tong hop) cho
     * 1 lo ban ghi - tranh N+1 query, cung 1 cach lam voi cac Danh muc khac (vd businessSegmentsById). */
    private ResponseContext buildResponseContext(UUID tenantId) {
        Map<UUID, String> engagementCodes = engagementRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .collect(Collectors.toMap(AuditEngagement::getId, AuditEngagement::getCode));
        Map<UUID, Employee> employees = employeeRepository.findByTenantIdOrderByFullNameAsc(tenantId).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        Map<UUID, String> usernames = userAccountRepository.findByEmployeeIdIn(employees.keySet()).stream()
                .collect(Collectors.toMap(UserAccount::getEmployeeId, UserAccount::getUsername, (a, b) -> a));
        Map<UUID, AuditProcessStepSummary> stepSummaries = processStepSummaryRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .collect(Collectors.toMap(AuditProcessStepSummary::getId, s -> s));
        return new ResponseContext(engagementCodes, employees, usernames, stepSummaries);
    }

    private record ResponseContext(Map<UUID, String> engagementCodes, Map<UUID, Employee> employees,
                                    Map<UUID, String> usernames, Map<UUID, AuditProcessStepSummary> stepSummaries) {
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("assignedUsername", "Username người được phân công"),
                new ExportColumn("processStepSummaryCode", "Mã BQT_TH"),
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("transactionDate", "Ngày giao dịch thực tế"),
                new ExportColumn("valueDate", "Ngày giá trị"),
                new ExportColumn("postingUser", "User hạch toán"),
                new ExportColumn("entryNumber", "Số bút toán"),
                new ExportColumn("customerCode", "Mã KH"),
                new ExportColumn("customerName", "Tên KH"),
                new ExportColumn("disbursementNumber", "Số giải ngân"),
                new ExportColumn("businessCode", "Mã nghiệp vụ"),
                new ExportColumn("transactionStatus", "Trạng thái giao dịch"),
                new ExportColumn("currency", "Loại tiền tệ"),
                new ExportColumn("debitAmount", "Số tiền ghi nợ"),
                new ExportColumn("creditAmount", "Số tiền ghi có"),
                new ExportColumn("accountNumber", "Tài khoản hạch toán"),
                new ExportColumn("postingDateDiff", "Chênh lệch ngày hạch toán"),
                new ExportColumn("ipcasReviewResult", "Kết quả rà soát trên IPCAS"),
                new ExportColumn("documentCheckResult", "Kết quả kiểm tra chứng từ"));
    }

    private List<Map<String, Object>> exportRows(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        ResponseContext ctx = buildResponseContext(tenantId);
        return repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId).stream()
                .map(item -> {
                    Employee assignee = item.getAssignedEmployeeId() == null ? null : ctx.employees().get(item.getAssignedEmployeeId());
                    AuditProcessStepSummary step = item.getProcessStepSummaryId() == null ? null : ctx.stepSummaries().get(item.getProcessStepSummaryId());
                    Map<String, Object> row = new HashMap<>();
                    row.put("assignedUsername", assignee == null ? null : ctx.usernames().get(assignee.getId()));
                    row.put("processStepSummaryCode", step == null ? null : step.getCode());
                    row.put("branchCode", item.getBranchCode());
                    row.put("transactionDate", item.getTransactionDate() == null ? null : item.getTransactionDate().format(DATE_FORMATS[0]));
                    row.put("valueDate", item.getValueDate() == null ? null : item.getValueDate().format(DATE_FORMATS[0]));
                    row.put("postingUser", item.getPostingUser());
                    row.put("entryNumber", item.getEntryNumber());
                    row.put("customerCode", item.getCustomerCode());
                    row.put("customerName", item.getCustomerName());
                    row.put("disbursementNumber", item.getDisbursementNumber());
                    row.put("businessCode", item.getBusinessCode());
                    row.put("transactionStatus", item.getTransactionStatus());
                    row.put("currency", item.getCurrency());
                    row.put("debitAmount", item.getDebitAmount());
                    row.put("creditAmount", item.getCreditAmount());
                    row.put("accountNumber", item.getAccountNumber());
                    row.put("postingDateDiff", item.getPostingDateDiff());
                    row.put("ipcasReviewResult", item.getIpcasReviewResult());
                    row.put("documentCheckResult", item.getDocumentCheckResult());
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

    private AuditCmTd2Response toResponse(AuditCmTd2 item, ResponseContext ctx) {
        Employee assignee = item.getAssignedEmployeeId() == null ? null : ctx.employees().get(item.getAssignedEmployeeId());
        AuditProcessStepSummary step = item.getProcessStepSummaryId() == null ? null : ctx.stepSummaries().get(item.getProcessStepSummaryId());
        return new AuditCmTd2Response(item.getId(),
                item.getEngagementId(), item.getEngagementId() == null ? null : ctx.engagementCodes().get(item.getEngagementId()),
                item.getAssignedEmployeeId(), assignee == null ? null : assignee.getEmployeeCode(),
                assignee == null ? null : ctx.usernames().get(assignee.getId()),
                item.getProcessStepSummaryId(), step == null ? null : step.getCode(), step == null ? null : step.getName(),
                item.getBranchCode(), item.getTransactionDate(), item.getValueDate(),
                item.getPostingUser(), item.getEntryNumber(), item.getCustomerCode(), item.getCustomerName(),
                item.getDisbursementNumber(), item.getBusinessCode(), item.getTransactionStatus(), item.getCurrency(),
                item.getDebitAmount(), item.getCreditAmount(), item.getAccountNumber(), item.getPostingDateDiff(),
                item.getIpcasReviewResult(), item.getDocumentCheckResult(), item.isActive());
    }
}
