package com.govia.audit.riskscoring.scoring.repository;

import com.govia.audit.riskscoring.scoring.entity.RiskAssessmentOtherHeader;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentOtherHeaderRepository extends JpaRepository<RiskAssessmentOtherHeader, UUID> {
    List<RiskAssessmentOtherHeader> findByTenantIdOrderByYearDescAuditObjectCodeAsc(UUID tenantId);

    Optional<RiskAssessmentOtherHeader> findByTenantIdAndAuditObjectCategoryIdAndAuditObjectCodeAndYear(
            UUID tenantId, UUID auditObjectCategoryId, String auditObjectCode, Integer year);
}
