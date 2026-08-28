package com.govia.audit.riskscoring.scoring.repository;

import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaOther;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskCriteriaOtherRepository extends JpaRepository<RiskCriteriaOther, UUID> {
    List<RiskCriteriaOther> findByTenantIdOrderByCodeAsc(UUID tenantId);

    List<RiskCriteriaOther> findByTenantIdAndAuditObjectCategoryIdOrderByCodeAsc(UUID tenantId, UUID auditObjectCategoryId);

    Optional<RiskCriteriaOther> findByTenantIdAndAuditObjectCategoryIdAndCode(UUID tenantId, UUID auditObjectCategoryId, String code);
}
