package com.govia.identity.service;

import com.govia.core.audit.AuditAction;
import com.govia.core.audit.AuditLogService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.ApprovalMatrixRuleRequest;
import com.govia.identity.dto.ApprovalMatrixRuleResponse;
import com.govia.identity.entity.ApprovalMatrixRule;
import com.govia.identity.entity.OrganizationUnit;
import com.govia.identity.repository.ApprovalMatrixRuleRepository;
import com.govia.identity.repository.OrganizationUnitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CRUD "ma tran phe duyet": moi quy tac quyet dinh dây phe duyet dong cho 1 don vi to chuc (hoac
 * quy tac mac dinh khi orgUnitId null) se di den dau thi dung (finalApprovalLevel) va co them buoc
 * Super Admin sau cung khong (requireFinalSuperAdminStep). EmployeeApprovalService doc lai qua
 * resolveActiveRuleForOrgUnit khi start quy trinh phe duyet nhan vien moi.
 */
@Service
public class ApprovalMatrixRuleService {

    /** Man hinh CRUD "Ma tran phe duyet" hien tai chi quan ly domain nay - cac domain khac (vd
     * AUDIT_WORKITEM) doc rule qua resolveActiveRuleForOrgUnit(tenantId, domain, orgUnitId) nhung
     * chua co man hinh quan tri rieng trong dot nay (fallback ve dây 1 buoc neu chua cau hinh). */
    private static final String EMPLOYEE_DOMAIN = "EMPLOYEE";

    private final ApprovalMatrixRuleRepository repository;
    private final OrganizationUnitRepository orgUnitRepository;
    private final AuditLogService auditLogService;

    public ApprovalMatrixRuleService(ApprovalMatrixRuleRepository repository, OrganizationUnitRepository orgUnitRepository,
                                      AuditLogService auditLogService) {
        this.repository = repository;
        this.orgUnitRepository = orgUnitRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ApprovalMatrixRuleResponse> list() {
        UUID tenantId = TenantContext.getTenantId();
        List<ApprovalMatrixRule> rules = repository.findByTenantIdAndDomain(tenantId, EMPLOYEE_DOMAIN);
        Map<UUID, OrganizationUnit> orgUnits = orgUnitRepository.findAllById(
                        rules.stream().map(ApprovalMatrixRule::getOrgUnitId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(OrganizationUnit::getId, u -> u));
        return rules.stream().map(r -> toResponse(r, orgUnits.get(r.getOrgUnitId()))).toList();
    }

    @Transactional
    public ApprovalMatrixRuleResponse create(ApprovalMatrixRuleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        validateOrgUnit(tenantId, request.orgUnitId());
        checkNoDuplicateScope(tenantId, request.orgUnitId(), null);

        ApprovalMatrixRule rule = new ApprovalMatrixRule();
        rule.setTenantId(tenantId);
        rule.setDomain(EMPLOYEE_DOMAIN);
        applyRequest(rule, request);
        rule = repository.save(rule);

        auditLogService.record("ApprovalMatrixRule", rule.getId(), AuditAction.CREATE,
                "Tao quy tac ma tran phe duyet cho " + scopeLabel(request.orgUnitId()));
        return toResponse(rule, request.orgUnitId() == null ? null : orgUnitRepository.findById(request.orgUnitId()).orElse(null));
    }

    @Transactional
    public ApprovalMatrixRuleResponse update(UUID id, ApprovalMatrixRuleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        ApprovalMatrixRule rule = getOwnedOrThrow(tenantId, id);
        validateOrgUnit(tenantId, request.orgUnitId());
        checkNoDuplicateScope(tenantId, request.orgUnitId(), id);

        applyRequest(rule, request);
        rule = repository.save(rule);

        auditLogService.record("ApprovalMatrixRule", rule.getId(), AuditAction.UPDATE,
                "Cap nhat quy tac ma tran phe duyet cho " + scopeLabel(request.orgUnitId()));
        return toResponse(rule, request.orgUnitId() == null ? null : orgUnitRepository.findById(request.orgUnitId()).orElse(null));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        ApprovalMatrixRule rule = getOwnedOrThrow(tenantId, id);
        repository.delete(rule);
        auditLogService.record("ApprovalMatrixRule", id, AuditAction.DELETE,
                "Xoa quy tac ma tran phe duyet cho " + scopeLabel(rule.getOrgUnitId()));
    }

    /** Uu tien quy tac rieng cho orgUnitId (neu co va active), sau do rot ve quy tac mac dinh
     * (orgUnitId null, active) - tra ve null neu chua cau hinh gi ca. Domain "EMPLOYEE" (man hinh
     * duyet nhan vien moi hien tai). */
    @Transactional(readOnly = true)
    public ApprovalMatrixRule resolveActiveRuleForOrgUnit(UUID tenantId, UUID orgUnitId) {
        return resolveActiveRuleForOrgUnit(tenantId, EMPLOYEE_DOMAIN, orgUnitId);
    }

    /** Ban tong quat: cho phep domain khac "EMPLOYEE" (vd "AUDIT_WORKITEM") dung CHUNG bang
     * approval_matrix_rule ma khong dung cham quy tac cua domain khac. Chua co man hinh quan tri
     * cho cac domain nay trong dot nay - tra ve null (chua cau hinh) neu khong tim thay, buoc goi
     * (vd AuditWorkApprovalChainResolver) tu quyet dinh fallback phu hop. */
    @Transactional(readOnly = true)
    public ApprovalMatrixRule resolveActiveRuleForOrgUnit(UUID tenantId, String domain, UUID orgUnitId) {
        if (orgUnitId != null) {
            Optional<ApprovalMatrixRule> specific = repository.findByTenantIdAndDomainAndOrgUnitId(tenantId, domain, orgUnitId)
                    .filter(ApprovalMatrixRule::isActive);
            if (specific.isPresent()) {
                return specific.get();
            }
        }
        return repository.findByTenantIdAndDomainAndOrgUnitIdIsNull(tenantId, domain).filter(ApprovalMatrixRule::isActive).orElse(null);
    }

    private void applyRequest(ApprovalMatrixRule rule, ApprovalMatrixRuleRequest request) {
        rule.setOrgUnitId(request.orgUnitId());
        rule.setFinalApprovalLevel(request.finalApprovalLevel());
        rule.setRequireFinalSuperAdminStep(request.requireFinalSuperAdminStep());
        rule.setActive(request.active());
    }

    private void validateOrgUnit(UUID tenantId, UUID orgUnitId) {
        if (orgUnitId == null) {
            return;
        }
        orgUnitRepository.findById(orgUnitId)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("ORG_UNIT_NOT_FOUND", "Don vi to chuc khong ton tai"));
    }

    private void checkNoDuplicateScope(UUID tenantId, UUID orgUnitId, UUID excludingId) {
        Optional<ApprovalMatrixRule> existing = orgUnitId == null
                ? repository.findByTenantIdAndDomainAndOrgUnitIdIsNull(tenantId, EMPLOYEE_DOMAIN)
                : repository.findByTenantIdAndDomainAndOrgUnitId(tenantId, EMPLOYEE_DOMAIN, orgUnitId);
        existing.filter(r -> excludingId == null || !r.getId().equals(excludingId))
                .ifPresent(r -> {
                    throw new BusinessException("APPROVAL_MATRIX_RULE_DUPLICATE_SCOPE",
                            "Da co quy tac cho " + scopeLabel(orgUnitId) + ", moi don vi (hoac mac dinh) chi duoc 1 quy tac");
                });
    }

    private String scopeLabel(UUID orgUnitId) {
        return orgUnitId == null ? "mac dinh (toan tenant)" : "don vi " + orgUnitId;
    }

    private ApprovalMatrixRule getOwnedOrThrow(UUID tenantId, UUID id) {
        return repository.findById(id)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("APPROVAL_MATRIX_RULE_NOT_FOUND", "Khong tim thay quy tac",
                        HttpStatus.NOT_FOUND));
    }

    private ApprovalMatrixRuleResponse toResponse(ApprovalMatrixRule rule, OrganizationUnit orgUnit) {
        return new ApprovalMatrixRuleResponse(rule.getId(), rule.getOrgUnitId(),
                orgUnit == null ? null : orgUnit.getCode(), orgUnit == null ? null : orgUnit.getName(),
                rule.getFinalApprovalLevel(), rule.isRequireFinalSuperAdminStep(), rule.isActive());
    }
}
