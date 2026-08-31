package com.govia.audit.finding.service;

import com.govia.audit.finding.dto.AuditFindingRequest;
import com.govia.audit.finding.dto.AuditFindingResponse;
import com.govia.audit.finding.entity.AuditFinding;
import com.govia.audit.finding.repository.AuditFindingRepository;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD cho "Phat hien kiem toan" (AuditFinding) - moi phat hien gan voi 1 chi nhanh (branchCode,
 * khong FK cung, xem AuditFinding). severity tham chieu AuditMasterDataItem (category RISK_LEVEL).
 *
 * <p>{@link #search} la method DUY NHAT doc du lieu - dung chung cho ca man hinh CRUD (khong loc) va
 * lop "Audit Tools" cho AI Agent sau nay (loc theo branchCode/khoang ngay/muc do) - tranh 2 noi doc
 * du lieu theo 2 cach khac nhau roi lech ket qua.
 */
@Service
public class AuditFindingService {

    private final AuditFindingRepository repository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final AuditMasterDataItemRepository masterDataItemRepository;
    private final AuditLogService auditLogService;

    public AuditFindingService(AuditFindingRepository repository, AuditObjectUnitRepository auditObjectUnitRepository,
                                AuditMasterDataItemRepository masterDataItemRepository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.masterDataItemRepository = masterDataItemRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AuditFindingResponse> list() {
        return search(null, null, null, null);
    }

    @Transactional(readOnly = true)
    public AuditFindingResponse get(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditFinding item = getOwnedOrThrow(tenantId, id);
        return toResponse(item, branchNamesByCode(tenantId), severityNamesByCode(tenantId));
    }

    @Transactional(readOnly = true)
    public List<AuditFindingResponse> search(String branchCode, LocalDate fromDate, LocalDate toDate, String severity) {
        UUID tenantId = TenantContext.getTenantId();
        Map<String, String> branchNames = branchNamesByCode(tenantId);
        Map<String, String> severityNames = severityNamesByCode(tenantId);
        return repository.findByTenantIdOrderByDetectedDateDesc(tenantId).stream()
                .filter(f -> branchCode == null || f.getBranchCode().equalsIgnoreCase(branchCode))
                .filter(f -> fromDate == null || !f.getDetectedDate().isBefore(fromDate))
                .filter(f -> toDate == null || !f.getDetectedDate().isAfter(toDate))
                .filter(f -> severity == null || f.getSeverity().equalsIgnoreCase(severity))
                .map(f -> toResponse(f, branchNames, severityNames))
                .toList();
    }

    @Transactional
    public AuditFindingResponse create(AuditFindingRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        validateBranch(tenantId, request.branchCode());
        validateSeverity(tenantId, request.severity());

        AuditFinding item = new AuditFinding();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditFinding", item.getId(), AuditAction.CREATE, "Tao phat hien kiem toan: " + item.getTitle());
        return toResponse(item, branchNamesByCode(tenantId), severityNamesByCode(tenantId));
    }

    @Transactional
    public AuditFindingResponse update(UUID id, AuditFindingRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditFinding item = getOwnedOrThrow(tenantId, id);
        validateBranch(tenantId, request.branchCode());
        validateSeverity(tenantId, request.severity());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditFinding", item.getId(), AuditAction.UPDATE, "Cap nhat phat hien kiem toan: " + item.getTitle());
        return toResponse(item, branchNamesByCode(tenantId), severityNamesByCode(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditFinding item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditFinding", id, AuditAction.DELETE, "Xoa phat hien kiem toan: " + item.getTitle());
    }

    private void applyRequest(AuditFinding item, AuditFindingRequest request) {
        item.setBranchCode(request.branchCode());
        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setSeverity(request.severity());
        item.setDetectedDate(request.detectedDate());
        item.setActive(request.active());
    }

    private void validateBranch(UUID tenantId, String branchCode) {
        auditObjectUnitRepository.findByTenantIdAndCode(tenantId, branchCode)
                .orElseThrow(() -> new BusinessException("AUDIT_FINDING_BRANCH_NOT_FOUND", "Khong tim thay chi nhanh: " + branchCode));
    }

    private void validateSeverity(UUID tenantId, String severity) {
        masterDataItemRepository.findByTenantIdAndCategoryAndCode(tenantId, AuditMasterDataCategory.RISK_LEVEL, severity)
                .orElseThrow(() -> new BusinessException("AUDIT_FINDING_SEVERITY_NOT_FOUND", "Khong tim thay muc do rui ro: " + severity));
    }

    private AuditFinding getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_FINDING_NOT_FOUND", "Khong tim thay phat hien kiem toan", HttpStatus.NOT_FOUND));
    }

    private Map<String, String> branchNamesByCode(UUID tenantId) {
        Map<String, String> map = new HashMap<>();
        for (AuditObjectUnit u : auditObjectUnitRepository.findByTenantIdOrderByCodeAsc(tenantId)) {
            map.put(u.getCode(), u.getName());
        }
        return map;
    }

    private Map<String, String> severityNamesByCode(UUID tenantId) {
        Map<String, String> map = new HashMap<>();
        masterDataItemRepository.findByTenantIdAndCategoryOrderBySortOrderAscNameAsc(tenantId, AuditMasterDataCategory.RISK_LEVEL)
                .forEach(item -> map.put(item.getCode(), item.getName()));
        return map;
    }

    private AuditFindingResponse toResponse(AuditFinding item, Map<String, String> branchNames, Map<String, String> severityNames) {
        return new AuditFindingResponse(item.getId(), item.getBranchCode(), branchNames.get(item.getBranchCode()),
                item.getTitle(), item.getDescription(), item.getSeverity(), severityNames.get(item.getSeverity()),
                item.getDetectedDate(), item.isActive());
    }
}
