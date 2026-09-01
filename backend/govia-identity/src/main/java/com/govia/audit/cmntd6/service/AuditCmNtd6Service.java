package com.govia.audit.cmntd6.service;

import com.govia.audit.cmntd6.dto.AuditCmNtd6Request;
import com.govia.audit.cmntd6.dto.AuditCmNtd6Response;
import com.govia.audit.cmntd6.entity.AuditCmNtd6;
import com.govia.audit.cmntd6.repository.AuditCmNtd6Repository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** CRUD + Import/Export cho "Danh sach chon mau User Ipcas AD, KPI CNTT" (sheet ZTC_CM_NTD6). */
@Service
public class AuditCmNtd6Service {

    private final AuditCmNtd6Repository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public AuditCmNtd6Service(AuditCmNtd6Repository repository, AuditLogService auditLogService,
                               ExcelExportService excelExportService, WordExportService wordExportService,
                               ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditCmNtd6Response> list() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditCmNtd6Response create(AuditCmNtd6Request request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.branchCode(), request.staffName(), request.ipcasUser(), null);

        AuditCmNtd6 item = new AuditCmNtd6();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd6", item.getId(), AuditAction.CREATE,
                "Tao ban ghi chon mau User Ipcas AD, KPI CNTT: " + item.getBranchCode() + " - " + item.getStaffName());
        return toResponse(item);
    }

    @Transactional
    public AuditCmNtd6Response update(UUID id, AuditCmNtd6Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd6 item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.branchCode(), request.staffName(), request.ipcasUser(), id);

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditCmNtd6", item.getId(), AuditAction.UPDATE,
                "Cap nhat ban ghi chon mau User Ipcas AD, KPI CNTT: " + item.getBranchCode() + " - " + item.getStaffName());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditCmNtd6 item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditCmNtd6", id, AuditAction.DELETE,
                "Xoa ban ghi chon mau User Ipcas AD, KPI CNTT: " + item.getBranchCode() + " - " + item.getStaffName());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("audit_cm_ntd6", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Danh sách chọn mẫu User Ipcas AD, KPI CNTT", exportColumns(), exportRows());
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
                String staffName = row.get("staffName");
                String ipcasUser = row.get("ipcasUser");
                if (isBlank(branchCode) || isBlank(staffName) || isBlank(ipcasUser)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu Ma chi nhanh, Ten can bo hoac User Ipcas");
                }

                Optional<AuditCmNtd6> existing = repository.findByTenantIdAndBranchCodeAndStaffNameAndIpcasUser(
                        tenantId, branchCode.trim(), staffName.trim(), ipcasUser.trim());
                AuditCmNtd6Request request = new AuditCmNtd6Request(branchCode.trim(), emptyToNull(row.get("staffCode")),
                        staffName.trim(), ipcasUser.trim(), emptyToNull(row.get("adUser")), emptyToNull(row.get("securityDevice")),
                        emptyToNull(row.get("sampleReason")), emptyToNull(row.get("sampleCode")), emptyToNull(row.get("auditResult")),
                        existing.map(AuditCmNtd6::isActive).orElse(true));
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

        auditLogService.record("AuditCmNtd6", null, AuditAction.CREATE,
                "Import Excel chon mau User Ipcas AD, KPI CNTT: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(AuditCmNtd6 item, AuditCmNtd6Request request) {
        item.setBranchCode(request.branchCode());
        item.setStaffCode(request.staffCode());
        item.setStaffName(request.staffName());
        item.setIpcasUser(request.ipcasUser());
        item.setAdUser(request.adUser());
        item.setSecurityDevice(request.securityDevice());
        item.setSampleReason(request.sampleReason());
        item.setSampleCode(request.sampleCode());
        item.setAuditResult(request.auditResult());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String branchCode, String staffName, String ipcasUser, UUID excludingId) {
        repository.findByTenantIdAndBranchCodeAndStaffNameAndIpcasUser(tenantId, branchCode, staffName, ipcasUser)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("AUDIT_CM_NTD6_DUPLICATE", "Ban ghi nay da ton tai: " + staffName);
                });
    }

    private AuditCmNtd6 getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_CM_NTD6_NOT_FOUND", "Khong tim thay ban ghi chon mau User Ipcas AD, KPI CNTT", HttpStatus.NOT_FOUND));
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("branchCode", "Mã chi nhánh"),
                new ExportColumn("staffCode", "Mã cán bộ"),
                new ExportColumn("staffName", "Tên cán bộ"),
                new ExportColumn("ipcasUser", "User Ipcas"),
                new ExportColumn("adUser", "User AD"),
                new ExportColumn("securityDevice", "Thiết bị bảo mật"),
                new ExportColumn("sampleReason", "Lý do chọn mẫu"),
                new ExportColumn("sampleCode", "Mã mẫu chọn"),
                new ExportColumn("auditResult", "Kết quả kiểm toán"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("branchCode", item.getBranchCode());
                    row.put("staffCode", item.getStaffCode());
                    row.put("staffName", item.getStaffName());
                    row.put("ipcasUser", item.getIpcasUser());
                    row.put("adUser", item.getAdUser());
                    row.put("securityDevice", item.getSecurityDevice());
                    row.put("sampleReason", item.getSampleReason());
                    row.put("sampleCode", item.getSampleCode());
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

    private AuditCmNtd6Response toResponse(AuditCmNtd6 item) {
        return new AuditCmNtd6Response(item.getId(), item.getBranchCode(), item.getStaffCode(), item.getStaffName(),
                item.getIpcasUser(), item.getAdUser(), item.getSecurityDevice(), item.getSampleReason(),
                item.getSampleCode(), item.getAuditResult(), item.isActive());
    }
}
