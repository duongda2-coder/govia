package com.govia.audit.tdkp.resolution;

import com.govia.audit.tdkp.common.TdkpStatus;
import com.govia.audit.tdkp.common.TdkpSupport;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.export.ExcelExportService;
import com.govia.core.export.ExcelImportService;
import com.govia.core.export.ExportColumn;
import com.govia.core.export.ImportResult;
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
import java.util.UUID;

/** Sheet 5 - ZTC_TDKP_NQ: quản lý, theo dõi và cập nhật tình hình thực hiện nghị quyết của HĐTV (nhập tay, import Excel, extract Excel). */
@Service
public class AuditTdkpResolutionService {

    private final AuditTdkpResolutionRepository repository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    public AuditTdkpResolutionService(AuditTdkpResolutionRepository repository, AuditLogService auditLogService,
                                      ExcelExportService excelExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<AuditTdkpResolutionDto.Response> list() {
        return repository.findByTenantIdOrderByIssueDateDescCodeDesc(TenantContext.getTenantId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuditTdkpResolutionDto.Response create(AuditTdkpResolutionDto.Request request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditTdkpResolution item = new AuditTdkpResolution();
        item.setTenantId(tenantId);
        item.setCode(TdkpSupport.nextCode("NQ", repository.findByTenantIdOrderByIssueDateDescCodeDesc(tenantId).stream().map(AuditTdkpResolution::getCode).toList()));
        apply(item, request);
        item = repository.save(item);
        auditLogService.record("AuditTdkpResolution", item.getId(), AuditAction.CREATE, "Tao theo doi nghi quyet HDTV: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public AuditTdkpResolutionDto.Response update(UUID id, AuditTdkpResolutionDto.Request request) {
        AuditTdkpResolution item = getOwnedOrThrow(id);
        apply(item, request);
        item = repository.save(item);
        auditLogService.record("AuditTdkpResolution", item.getId(), AuditAction.UPDATE, "Cap nhat theo doi nghi quyet HDTV: " + item.getCode());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        AuditTdkpResolution item = getOwnedOrThrow(id);
        repository.delete(item);
        auditLogService.record("AuditTdkpResolution", id, AuditAction.DELETE, "Xoa theo doi nghi quyet HDTV: " + item.getCode());
    }

    private List<ExportColumn> columns() {
        return List.of(new ExportColumn("code", "Mã quản lý kiến nghị nghị quyết"), new ExportColumn("resolutionNumber", "SỐ NQ"),
                new ExportColumn("issueDate", "NGÀY BAN HÀNH"), new ExportColumn("content", "NỘI DUNG NGHỊ QUYẾT"),
                new ExportColumn("implementation", "TÌNH HÌNH THỰC HIỆN"), new ExportColumn("status", "ĐÁNH GIÁ (Đã/Đang/Chưa thực hiện)"),
                new ExportColumn("supervisor", "NGƯỜI GIÁM SÁT"), new ExportColumn("issuanceEvaluation", "ĐÁNH GIÁ VỀ VIỆC BAN HÀNH NGHỊ QUYẾT CỦA HĐTV"),
                new ExportColumn("note", "Ghi chú"));
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        List<Map<String, Object>> rows = list().stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("code", r.code());
            row.put("resolutionNumber", r.resolutionNumber());
            row.put("issueDate", r.issueDate());
            row.put("content", r.content());
            row.put("implementation", r.implementation());
            row.put("status", r.statusLabel());
            row.put("supervisor", r.supervisor());
            row.put("issuanceEvaluation", r.issuanceEvaluation());
            row.put("note", r.note());
            return row;
        }).toList();
        return excelExportService.export("TDKP_NQ", columns(), rows);
    }

    /** Import: có Mã quản lý đã tồn tại thì cập nhật, ngược lại tạo mới (mã tự sinh); "Đánh giá" phải là Đã/Đang/Chưa thực hiện. */
    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = excelImportService.parse(file.getInputStream(), columns());
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file", e);
        }
        UUID tenantId = TenantContext.getTenantId();
        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                if (TdkpSupport.isBlank(row.get("resolutionNumber"))) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu So NQ");
                }
                AuditTdkpResolutionDto.Request request = new AuditTdkpResolutionDto.Request(row.get("resolutionNumber").trim(),
                        TdkpSupport.parseDate(row.get("issueDate")), TdkpSupport.emptyToNull(row.get("content")), TdkpSupport.emptyToNull(row.get("implementation")),
                        TdkpStatus.parse(row.get("status")), TdkpSupport.emptyToNull(row.get("supervisor")), TdkpSupport.emptyToNull(row.get("issuanceEvaluation")),
                        TdkpSupport.emptyToNull(row.get("note")));
                String code = TdkpSupport.emptyToNull(row.get("code"));
                AuditTdkpResolution existing = code == null ? null : repository.findByTenantIdAndCode(tenantId, code).orElse(null);
                if (existing != null) {
                    update(existing.getId(), request);
                } else {
                    create(request);
                }
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(i + 2, e.getMessage()));
            }
        }
        auditLogService.record("AuditTdkpResolution", null, AuditAction.CREATE, "Import Excel theo doi nghi quyet HDTV: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void apply(AuditTdkpResolution item, AuditTdkpResolutionDto.Request request) {
        item.setResolutionNumber(request.resolutionNumber().trim());
        item.setIssueDate(request.issueDate());
        item.setContent(TdkpSupport.emptyToNull(request.content()));
        item.setImplementation(TdkpSupport.emptyToNull(request.implementation()));
        item.setStatus(request.status());
        item.setSupervisor(TdkpSupport.emptyToNull(request.supervisor()));
        item.setIssuanceEvaluation(TdkpSupport.emptyToNull(request.issuanceEvaluation()));
        item.setNote(TdkpSupport.emptyToNull(request.note()));
    }

    private AuditTdkpResolution getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findById(id).filter(i -> i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("TDKP_RESOLUTION_NOT_FOUND", "Khong tim thay nghi quyet", HttpStatus.NOT_FOUND));
    }

    private AuditTdkpResolutionDto.Response toResponse(AuditTdkpResolution item) {
        return new AuditTdkpResolutionDto.Response(item.getId(), item.getCode(), item.getResolutionNumber(), item.getIssueDate(), item.getContent(),
                item.getImplementation(), item.getStatus(), TdkpStatus.labelOf(item.getStatus()), item.getSupervisor(), item.getIssuanceEvaluation(), item.getNote());
    }
}
