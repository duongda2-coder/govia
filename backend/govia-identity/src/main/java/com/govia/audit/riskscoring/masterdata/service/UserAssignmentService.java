package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.UserAssignmentRequest;
import com.govia.audit.riskscoring.masterdata.dto.UserAssignmentResponse;
import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQuantitative;
import com.govia.audit.riskscoring.masterdata.entity.RiskUserAssignment;
import com.govia.audit.riskscoring.masterdata.repository.RiskCriteriaQuantitativeRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskUserAssignmentRepository;
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
import java.util.UUID;

/** CRUD + Import/Export cho danh muc "Phan quyen User theo chi tieu dinh luong" (sheet ZTC_HSRR_DL_User). */
@Service
public class UserAssignmentService {

    private final RiskUserAssignmentRepository repository;
    private final RiskCriteriaQuantitativeRepository criteriaRepository;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final ExcelImportService excelImportService;

    public UserAssignmentService(RiskUserAssignmentRepository repository, RiskCriteriaQuantitativeRepository criteriaRepository,
                                  AuditLogService auditLogService, ExcelExportService excelExportService,
                                  WordExportService wordExportService, ExcelImportService excelImportService) {
        this.repository = repository;
        this.criteriaRepository = criteriaRepository;
        this.auditLogService = auditLogService;
        this.excelExportService = excelExportService;
        this.wordExportService = wordExportService;
        this.excelImportService = excelImportService;
    }

    @Transactional(readOnly = true)
    public List<UserAssignmentResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, String> criteriaCodes = criteriaCodesById(tenantId);
        return repository.findByTenantIdOrderByUsernameAsc(tenantId).stream().map(item -> toResponse(item, criteriaCodes)).toList();
    }

    @Transactional
    public UserAssignmentResponse create(UserAssignmentRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        checkNoDuplicate(tenantId, request.username(), request.criteriaId(), request.branchCode(), null);
        RiskCriteriaQuantitative criteria = getCriteriaOrThrow(tenantId, request.criteriaId());

        RiskUserAssignment item = new RiskUserAssignment();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskUserAssignment", item.getId(), AuditAction.CREATE, "Tao phan quyen user: " + item.getUsername());
        return toResponse(item, Map.of(criteria.getId(), criteria.getCode()));
    }

    @Transactional
    public UserAssignmentResponse update(UUID id, UserAssignmentRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        RiskUserAssignment item = getOwnedOrThrow(tenantId, id);
        checkNoDuplicate(tenantId, request.username(), request.criteriaId(), request.branchCode(), id);
        RiskCriteriaQuantitative criteria = getCriteriaOrThrow(tenantId, request.criteriaId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("RiskUserAssignment", item.getId(), AuditAction.UPDATE, "Cap nhat phan quyen user: " + item.getUsername());
        return toResponse(item, Map.of(criteria.getId(), criteria.getCode()));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        RiskUserAssignment item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("RiskUserAssignment", id, AuditAction.DELETE, "Xoa phan quyen user: " + item.getUsername());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        return excelExportService.export("risk_score_user_assignment", exportColumns(), exportRows());
    }

    @Transactional(readOnly = true)
    public byte[] exportWord() {
        return wordExportService.export("Phân quyền User theo chỉ tiêu định lượng", exportColumns(), exportRows());
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
        Map<String, UUID> criteriaIdsByCode = new HashMap<>();
        criteriaRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> criteriaIdsByCode.put(c.getCode(), c.getId()));

        int success = 0;
        List<ImportResult.ImportRowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            try {
                String username = row.get("username");
                String criteriaCode = row.get("criteriaCode");
                if (isBlank(username) || isBlank(criteriaCode)) {
                    throw new BusinessException("IMPORT_MISSING_REQUIRED", "Thieu User hoac Ma chi tieu");
                }
                UUID criteriaId = criteriaIdsByCode.get(criteriaCode.trim());
                if (criteriaId == null) {
                    throw new BusinessException("RISK_CRITERIA_DL_NOT_FOUND", "Khong tim thay chi tieu dinh luong: " + criteriaCode);
                }
                create(new UserAssignmentRequest(username.trim(), criteriaId, emptyToNull(row.get("branchCode")),
                        emptyToNull(row.get("classification")), true));
                success++;
            } catch (Exception e) {
                errors.add(new ImportResult.ImportRowError(rowNumber, e.getMessage()));
            }
        }

        auditLogService.record("RiskUserAssignment", null, AuditAction.CREATE,
                "Import Excel phan quyen user: " + success + " thanh cong, " + errors.size() + " loi");
        return new ImportResult(success, errors.size(), errors);
    }

    private void applyRequest(RiskUserAssignment item, UserAssignmentRequest request) {
        item.setUsername(request.username());
        item.setCriteriaId(request.criteriaId());
        item.setBranchCode(request.branchCode());
        item.setClassification(request.classification());
        item.setActive(request.active());
    }

    private void checkNoDuplicate(UUID tenantId, String username, UUID criteriaId, String branchCode, UUID excludingId) {
        repository.findByTenantIdAndUsernameAndCriteriaIdAndBranchCode(tenantId, username, criteriaId, branchCode)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new BusinessException("RISK_USER_ASSIGN_DUPLICATE", "User " + username + " da duoc gan chi tieu nay");
                });
    }

    private RiskCriteriaQuantitative getCriteriaOrThrow(UUID tenantId, UUID criteriaId) {
        return criteriaRepository.findById(criteriaId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_CRITERIA_DL_NOT_FOUND", "Khong tim thay chi tieu dinh luong"));
    }

    private RiskUserAssignment getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("RISK_USER_ASSIGN_NOT_FOUND", "Khong tim thay phan quyen user", HttpStatus.NOT_FOUND));
    }

    private Map<UUID, String> criteriaCodesById(UUID tenantId) {
        Map<UUID, String> map = new HashMap<>();
        criteriaRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(c -> map.put(c.getId(), c.getCode()));
        return map;
    }

    private List<ExportColumn> exportColumns() {
        return List.of(
                new ExportColumn("username", "User"),
                new ExportColumn("criteriaCode", "Ma chi tieu"),
                new ExportColumn("branchCode", "Chi nhanh"),
                new ExportColumn("classification", "Phan loai"));
    }

    private List<Map<String, Object>> exportRows() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, String> criteriaCodes = criteriaCodesById(tenantId);
        return repository.findByTenantIdOrderByUsernameAsc(tenantId).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("username", item.getUsername());
                    row.put("criteriaCode", criteriaCodes.get(item.getCriteriaId()));
                    row.put("branchCode", item.getBranchCode());
                    row.put("classification", item.getClassification());
                    return row;
                }).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private UserAssignmentResponse toResponse(RiskUserAssignment item, Map<UUID, String> criteriaCodes) {
        return new UserAssignmentResponse(item.getId(), item.getUsername(), item.getCriteriaId(),
                criteriaCodes.get(item.getCriteriaId()), item.getBranchCode(), item.getClassification(), item.isActive());
    }
}
