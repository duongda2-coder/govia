package com.govia.audit.tdkp.resolution;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.tdkp.common.TdkpMasterData;
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

    private static final String FOLLOW_UP_GROUP_QTDH = "QTDH";
    private static final String FOLLOW_UP_GROUP_TD = "TD";
    private static final String FOLLOW_UP_GROUP_NTD = "NTD";

    private final AuditTdkpResolutionRepository repository;
    private final TdkpMasterData masterData;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    public AuditTdkpResolutionService(AuditTdkpResolutionRepository repository, TdkpMasterData masterData, AuditLogService auditLogService,
                                      ExcelExportService excelExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.masterData = masterData;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
    }

    /** "Đơn vị" - chi liet ke doi tuong kiem toan co loai doi tuong = HO (Hoi so). */
    private static String followUpGroupLabel(String group) {
        if (group == null) {
            return null;
        }
        return switch (group) {
            case FOLLOW_UP_GROUP_QTDH -> "QTĐH";
            case FOLLOW_UP_GROUP_TD -> "TD";
            case FOLLOW_UP_GROUP_NTD -> "NTD";
            default -> group;
        };
    }

    @Transactional(readOnly = true)
    public List<AuditTdkpResolutionDto.Response> list() {
        Map<UUID, AuditObjectUnit> units = masterData.units();
        return repository.findByTenantIdOrderByIssueDateDescCodeDesc(TenantContext.getTenantId()).stream().map(item -> toResponse(item, units)).toList();
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
        return toResponse(item, masterData.units());
    }

    @Transactional
    public AuditTdkpResolutionDto.Response update(UUID id, AuditTdkpResolutionDto.Request request) {
        AuditTdkpResolution item = getOwnedOrThrow(id);
        apply(item, request);
        item = repository.save(item);
        auditLogService.record("AuditTdkpResolution", item.getId(), AuditAction.UPDATE, "Cap nhat theo doi nghi quyet HDTV: " + item.getCode());
        return toResponse(item, masterData.units());
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
                new ExportColumn("workDetail", "Công việc chi tiết"), new ExportColumn("fieldArea", "Lĩnh vực"), new ExportColumn("unit", "Đơn vị"),
                new ExportColumn("contactPerson", "Người liên hệ"), new ExportColumn("relatedResolution", "NQ liên quan có cùng ND"),
                new ExportColumn("completionDeadline", "Thời hạn hoàn thành"), new ExportColumn("completionDeadlineBasis", "Căn cứ thời hạn hoàn thành"),
                new ExportColumn("implementation", "TÌNH HÌNH THỰC HIỆN"), new ExportColumn("progressStatus", "Tình trạng"),
                new ExportColumn("reason", "Lý do"), new ExportColumn("issuanceEvaluation", "Đánh giá việc ban hành NQ"),
                new ExportColumn("completionDate", "Thời gian hoàn thành"), new ExportColumn("resolutionState", "Trạng thái NQ"),
                new ExportColumn("followUpGroup", "Nhóm theo dõi"), new ExportColumn("followerName", "Người theo dõi"),
                new ExportColumn("proposal", "Đề xuất"), new ExportColumn("proposalReason", "Lý do đề xuất"), new ExportColumn("note", "Ghi chú"),
                new ExportColumn("approvalStatus", "Trạng thái phê duyệt"));
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        List<Map<String, Object>> rows = list().stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("code", r.code());
            row.put("resolutionNumber", r.resolutionNumber());
            row.put("issueDate", r.issueDate());
            row.put("content", r.content());
            row.put("workDetail", r.workDetail());
            row.put("fieldArea", r.fieldArea());
            row.put("unit", r.unitName());
            row.put("contactPerson", r.contactPerson());
            row.put("relatedResolution", r.relatedResolution());
            row.put("completionDeadline", r.completionDeadline());
            row.put("completionDeadlineBasis", r.completionDeadlineBasis());
            row.put("implementation", r.implementation());
            row.put("progressStatus", r.progressStatusLabel());
            row.put("reason", r.reason());
            row.put("issuanceEvaluation", r.issuanceEvaluation());
            row.put("completionDate", r.completionDate());
            row.put("resolutionState", r.resolutionStateLabel());
            row.put("followUpGroup", r.followUpGroupLabel());
            row.put("followerName", r.followerName());
            row.put("proposal", r.proposal());
            row.put("proposalReason", r.proposalReason());
            row.put("note", r.note());
            row.put("approvalStatus", r.approvalStatusLabel());
            return row;
        }).toList();
        return excelExportService.export("TDKP_NQ", columns(), rows);
    }

    /** Import: có Mã quản lý đã tồn tại thì cập nhật, ngược lại tạo mới (mã tự sinh); "Tình trạng" phải là Đã/Đang/Chưa thực hiện. */
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
                        TdkpSupport.parseDate(row.get("issueDate")), TdkpSupport.emptyToNull(row.get("content")),
                        TdkpSupport.emptyToNull(row.get("workDetail")), TdkpSupport.emptyToNull(row.get("fieldArea")),
                        masterData.resolveUnit(row.get("unit"), "Don vi"), TdkpSupport.emptyToNull(row.get("contactPerson")),
                        TdkpSupport.emptyToNull(row.get("relatedResolution")), TdkpSupport.parseDate(row.get("completionDeadline")),
                        TdkpSupport.emptyToNull(row.get("completionDeadlineBasis")), TdkpSupport.emptyToNull(row.get("implementation")),
                        TdkpStatus.parse(row.get("progressStatus")), TdkpSupport.emptyToNull(row.get("reason")),
                        TdkpSupport.emptyToNull(row.get("issuanceEvaluation")), TdkpSupport.parseDate(row.get("completionDate")),
                        TdkpSupport.emptyToNull(row.get("followUpGroup")), TdkpSupport.emptyToNull(row.get("followerName")),
                        TdkpSupport.emptyToNull(row.get("proposal")), TdkpSupport.emptyToNull(row.get("proposalReason")),
                        TdkpSupport.emptyToNull(row.get("note")), TdkpSupport.parseApprovalStatus(row.get("approvalStatus")));
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
        item.setWorkDetail(TdkpSupport.emptyToNull(request.workDetail()));
        item.setFieldArea(TdkpSupport.emptyToNull(request.fieldArea()));
        item.setUnitId(masterData.requireUnit(request.unitId(), "Don vi"));
        item.setContactPerson(TdkpSupport.emptyToNull(request.contactPerson()));
        item.setRelatedResolution(TdkpSupport.emptyToNull(request.relatedResolution()));
        item.setCompletionDeadline(request.completionDeadline());
        item.setCompletionDeadlineBasis(TdkpSupport.emptyToNull(request.completionDeadlineBasis()));
        item.setImplementation(TdkpSupport.emptyToNull(request.implementation()));
        item.setProgressStatus(request.progressStatus());
        item.setReason(TdkpSupport.emptyToNull(request.reason()));
        item.setIssuanceEvaluation(TdkpSupport.emptyToNull(request.issuanceEvaluation()));
        item.setCompletionDate(request.completionDate());
        item.setFollowUpGroup(TdkpSupport.emptyToNull(request.followUpGroup()));
        item.setFollowerName(TdkpSupport.emptyToNull(request.followerName()));
        item.setProposal(TdkpSupport.emptyToNull(request.proposal()));
        item.setProposalReason(TdkpSupport.emptyToNull(request.proposalReason()));
        item.setNote(TdkpSupport.emptyToNull(request.note()));
        item.setApprovalStatus(request.approvalStatus());
    }

    private AuditTdkpResolution getOwnedOrThrow(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findById(id).filter(i -> i.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("TDKP_RESOLUTION_NOT_FOUND", "Khong tim thay nghi quyet", HttpStatus.NOT_FOUND));
    }

    private AuditTdkpResolutionDto.Response toResponse(AuditTdkpResolution item, Map<UUID, AuditObjectUnit> units) {
        AuditObjectUnit unit = item.getUnitId() == null ? null : units.get(item.getUnitId());
        String state = TdkpSupport.deadlineState(item.getCompletionDeadline());
        return new AuditTdkpResolutionDto.Response(item.getId(), item.getCode(), item.getResolutionNumber(), item.getIssueDate(), item.getContent(),
                item.getWorkDetail(), item.getFieldArea(), item.getUnitId(), unit == null ? null : unit.getCode(), unit == null ? null : unit.getName(),
                item.getContactPerson(), item.getRelatedResolution(), item.getCompletionDeadline(), item.getCompletionDeadlineBasis(), item.getImplementation(),
                item.getProgressStatus(), TdkpStatus.labelOf(item.getProgressStatus()), item.getReason(), item.getIssuanceEvaluation(), item.getCompletionDate(),
                state, TdkpSupport.deadlineLabel(state), item.getFollowUpGroup(), followUpGroupLabel(item.getFollowUpGroup()), item.getFollowerName(),
                item.getProposal(), item.getProposalReason(), item.getNote(), item.getApprovalStatus(), TdkpSupport.approvalStatusLabel(item.getApprovalStatus()));
    }
}
