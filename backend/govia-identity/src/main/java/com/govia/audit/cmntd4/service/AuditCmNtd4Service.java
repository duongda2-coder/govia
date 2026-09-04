package com.govia.audit.cmntd4.service;

import com.govia.audit.cmntd4.dto.AuditCmNtd4Request;
import com.govia.audit.cmntd4.dto.AuditCmNtd4Response;
import com.govia.audit.cmntd4.entity.AuditCmNtd4;
import com.govia.audit.cmntd4.repository.AuditCmNtd4Repository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** CRUD + Import/Export cho "Danh sach chon mau LC va nho thu TTQT" (sheet ZTC_CM_NTD4). */
@Service
public class AuditCmNtd4Service {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
    };

    private final AuditCmNtd4Repository repository;
    private final AuditEngagementRepository engagementRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditProcessStepSummaryRepository processStepSummaryRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmNtd4Service(AuditCmNtd4Repository repository, AuditEngagementRepository engagementRepository,
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
    public List<AuditCmNtd4Response> list(UUID engagementId) {
        UUID tenantId = TenantContext.getTenantId();
        ResponseContext ctx = buildResponseContext(tenantId);
        return repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId).stream()
                .map(item -> toResponse(item, ctx)).toList();
    }

    @Transactional
    public AuditCmNtd4Response create(AuditCmNtd4Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.referenceNumber(), request.openDate(), request.corebankCustomerCode(), null);
        validateEngagement(tenantId, request.engagementId());

        AuditCmNtd4 item = new AuditCmNtd4();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd4", item.getId(), AuditAction.CREATE,
                "Tao ban ghi chon mau LC va nho thu TTQT: " + item.getBranchCode() + " - " + item.getCorebankCustomerCode());
        return toResponse(item, buildResponseContext(tenantId));
    }

    @Transactional
    public AuditCmNtd4Response update(UUID id, AuditCmNtd4Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd4 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.referenceNumber(), request.openDate(), request.corebankCustomerCode(), id);
        validateEngagement(tenantId, request.engagementId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd4", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi chon mau LC va nho thu TTQT: " + item.getBranchCode() + " - " + item.getCorebankCustomerCode());
        return toResponse(item, buildResponseContext(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd4 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmNtd4", id, AuditAction.DELETE,
                "Xoa ban ghi chon mau LC va nho thu TTQT: " + item.getBranchCode() + " - " + item.getCorebankCustomerCode());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(UUID engagementId) {
        return excelExportService.export("audit_cm_ntd4", exportColumns(), exportRows(engagementId));
    }

    @Transactional(readOnly = true)
    public byte[] exportWord(UUID engagementId) {
        return wordExportService.export("Danh sách chọn mẫu LC và nhờ thu TTQT", exportColumns(), exportRows(engagementId));
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
                String corebankCustomerCode = row.get("corebankCustomerCode");
                BigDecimal referenceNumber = parseDecimal(row.get("referenceNumber"));
                LocalDate openDate = parseDate(row.get("openDate"));
                if (isBlank(branchCode) || referenceNumber == null || openDate == null) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh, So tham chieu hoac Ngay mo");
                }
                String assignedUsername = row.get("assignedUsername");
                String stepSummaryCode = row.get("processStepSummaryCode");

                Optional<AuditCmNtd4> existing = repository.findByTenantIdAndBranchCodeAndReferenceNumberAndOpenDateAndCorebankCustomerCode(
                        tenantId, branchCode.trim(), referenceNumber, openDate, emptyToNull(corebankCustomerCode));
                AuditCmNtd4Request request = new AuditCmNtd4Request(engagementId,
                        isBlank(assignedUsername) ? null : employeeIdsByUsername.get(assignedUsername.trim()),
                        isBlank(stepSummaryCode) ? null : stepSummaryIdsByCode.get(stepSummaryCode.trim()),
                        branchCode.trim(), referenceNumber, openDate,
                        emptyToNull(corebankCustomerCode), parseDecimal(row.get("amount")),
                        emptyToNull(row.get("beneficiary")), emptyToNull(row.get("sampleReason")), emptyToNull(row.get("auditResult")), emptyToNull(row.get("recommendationType")),
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
        item.setEngagementId(request.engagementId());
        item.setAssignedEmployeeId(request.assignedEmployeeId());
        item.setProcessStepSummaryId(request.processStepSummaryId());
        item.setBranchCode(request.branchCode());
        item.setReferenceNumber(request.referenceNumber());
        item.setOpenDate(request.openDate());
        item.setCorebankCustomerCode(request.corebankCustomerCode());
        item.setAmount(request.amount());
        item.setBeneficiary(request.beneficiary());
        item.setSampleReason(request.sampleReason());
        item.setAuditResult(request.auditResult());
        item.setRecommendationType(request.recommendationType());
        item.setTransactionStaff(request.transactionStaff());
        item.setControlUser(request.controlUser());
        item.setControlStaff(request.controlStaff());
        item.setControlStaffTitle(request.controlStaffTitle());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, BigDecimal referenceNumber, LocalDate openDate, String corebankCustomerCode, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndReferenceNumberAndOpenDateAndCorebankCustomerCode(tenantId, branchCode, referenceNumber, openDate, corebankCustomerCode)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_NTD4_DUPLICATE", "Ban ghi nay da ton tai: " + referenceNumber);
                });
    }

    private AuditCmNtd4 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_NTD4_NOT_FOUND", "Khong tim thay ban ghi chon mau LC va nho thu TTQT", HttpStatus.NOT_FOUND));
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
                new ExportColumn("referenceNumber", "Số tham chiếu"),
                new ExportColumn("openDate", "Ngày mở"),
                new ExportColumn("corebankCustomerCode", "Mã KH của Corebank"),
                new ExportColumn("amount", "Số tiền"),
                new ExportColumn("beneficiary", "Người thụ hưởng"),
                new ExportColumn("sampleReason", "Lý do chọn mẫu"),
                new ExportColumn("auditResult", "Kết quả kiểm toán"),
                new ExportColumn("recommendationType", "Dạng kiến nghị"),
                new ExportColumn("transactionStaff", "Cán bộ giao dịch"),
                new ExportColumn("controlUser", "User kiểm soát"),
                new ExportColumn("controlStaff", "Cán bộ kiểm soát"),
                new ExportColumn("controlStaffTitle", "Chức danh người kiểm soát"));
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
                    row.put("referenceNumber", item.getReferenceNumber());
                    row.put("openDate", item.getOpenDate() == null ? null : item.getOpenDate().format(DATE_FORMATS[0]));
                    row.put("corebankCustomerCode", item.getCorebankCustomerCode());
                    row.put("amount", item.getAmount());
                    row.put("beneficiary", item.getBeneficiary());
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

    private AuditCmNtd4Response toResponse(AuditCmNtd4 item, ResponseContext ctx) {
        Employee assignee = item.getAssignedEmployeeId() == null ? null : ctx.employees().get(item.getAssignedEmployeeId());
        AuditProcessStepSummary step = item.getProcessStepSummaryId() == null ? null : ctx.stepSummaries().get(item.getProcessStepSummaryId());
        return new AuditCmNtd4Response(item.getId(),
                item.getEngagementId(), item.getEngagementId() == null ? null : ctx.engagementCodes().get(item.getEngagementId()),
                item.getAssignedEmployeeId(), assignee == null ? null : assignee.getEmployeeCode(),
                assignee == null ? null : ctx.usernames().get(assignee.getId()),
                item.getProcessStepSummaryId(), step == null ? null : step.getCode(), step == null ? null : step.getName(),
                item.getBranchCode(), item.getReferenceNumber(), item.getOpenDate(),
                item.getCorebankCustomerCode(), item.getAmount(), item.getBeneficiary(), item.getSampleReason(), item.getAuditResult(),
                item.getRecommendationType(), item.getTransactionStaff(), item.getControlUser(), item.getControlStaff(),
                item.getControlStaffTitle(), item.isActive());
    }
}
