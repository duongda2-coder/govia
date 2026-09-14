package com.govia.audit.riskscoring.masterdata.service;

import com.govia.audit.riskscoring.masterdata.dto.AuditObjectInspectionHistoryRequest;
import com.govia.audit.riskscoring.masterdata.dto.AuditObjectInspectionHistoryResponse;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectInspectionHistory;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectInspectionHistoryRepository;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** CRUD cho "Lich su KT" cua 1 Doi tuong kiem toan - Don vi (sheet ZTC_DTKT1, nut "Lich su KT") -
 * nhung nut/tab nhung trong man hinh Doi tuong kiem toan (AuditObjectUnitTable), khong phai man
 * hinh rieng. Dung chung quyen AUDIT.RISK_SCORING.* voi man hinh cha. */
@Service
public class AuditObjectInspectionHistoryService {

    private final AuditObjectInspectionHistoryRepository repository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final AuditLogService auditLogService;

    public AuditObjectInspectionHistoryService(AuditObjectInspectionHistoryRepository repository,
                                                AuditObjectUnitRepository auditObjectUnitRepository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AuditObjectInspectionHistoryResponse> listByUnit(UUID auditObjectUnitId) {
        UUID tenantId = TenantContext.getTenantId();
        return repository.findByTenantIdAndAuditObjectUnitIdOrderByYearDesc(tenantId, auditObjectUnitId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public AuditObjectInspectionHistoryResponse create(AuditObjectInspectionHistoryRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        validateAuditObjectUnit(tenantId, request.auditObjectUnitId());

        AuditObjectInspectionHistory item = new AuditObjectInspectionHistory();
        item.setTenantId(tenantId);
        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditObjectInspectionHistory", item.getId(), AuditAction.CREATE,
                "Tao lich su KT: " + item.getInspectionType() + " nam " + item.getYear());
        return toResponse(item);
    }

    @Transactional
    public AuditObjectInspectionHistoryResponse update(UUID id, AuditObjectInspectionHistoryRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectInspectionHistory item = getOwnedOrThrow(tenantId, id);
        validateAuditObjectUnit(tenantId, request.auditObjectUnitId());

        applyRequest(item, request);
        item = repository.save(item);

        auditLogService.record("AuditObjectInspectionHistory", item.getId(), AuditAction.UPDATE,
                "Cap nhat lich su KT: " + item.getInspectionType() + " nam " + item.getYear());
        return toResponse(item);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        AuditObjectInspectionHistory item = getOwnedOrThrow(tenantId, id);
        repository.delete(item);
        auditLogService.record("AuditObjectInspectionHistory", id, AuditAction.DELETE,
                "Xoa lich su KT: " + item.getInspectionType() + " nam " + item.getYear());
    }

    private void applyRequest(AuditObjectInspectionHistory item, AuditObjectInspectionHistoryRequest request) {
        item.setAuditObjectUnitId(request.auditObjectUnitId());
        item.setInspectionType(request.inspectionType());
        item.setYear(request.year());
        item.setNote(request.note());
    }

    private void validateAuditObjectUnit(UUID tenantId, UUID auditObjectUnitId) {
        auditObjectUnitRepository.findById(auditObjectUnitId)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_UNIT_NOT_FOUND", "Khong tim thay doi tuong kiem toan"));
    }

    private AuditObjectInspectionHistory getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(item -> item.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("AUDIT_OBJECT_INSPECTION_HISTORY_NOT_FOUND", "Khong tim thay lich su KT", HttpStatus.NOT_FOUND));
    }

    private AuditObjectInspectionHistoryResponse toResponse(AuditObjectInspectionHistory item) {
        return new AuditObjectInspectionHistoryResponse(item.getId(), item.getAuditObjectUnitId(), item.getInspectionType(),
                item.getYear(), item.getNote());
    }
}
