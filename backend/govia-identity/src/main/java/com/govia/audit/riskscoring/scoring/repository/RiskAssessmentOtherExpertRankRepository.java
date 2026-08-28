package com.govia.audit.riskscoring.scoring.repository;

import com.govia.audit.riskscoring.scoring.entity.RiskAssessmentOtherExpertRank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentOtherExpertRankRepository extends JpaRepository<RiskAssessmentOtherExpertRank, UUID> {
    List<RiskAssessmentOtherExpertRank> findByTenantIdAndYearOrderByRiskScoreDesc(UUID tenantId, Integer year);

    Optional<RiskAssessmentOtherExpertRank> findByTenantIdAndAuditObjectCategoryIdAndAuditObjectCodeAndYear(
            UUID tenantId, UUID auditObjectCategoryId, String auditObjectCode, Integer year);
}
