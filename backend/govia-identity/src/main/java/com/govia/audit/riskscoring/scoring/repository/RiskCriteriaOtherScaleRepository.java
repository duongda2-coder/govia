package com.govia.audit.riskscoring.scoring.repository;

import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaOtherScale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskCriteriaOtherScaleRepository extends JpaRepository<RiskCriteriaOtherScale, UUID> {
    List<RiskCriteriaOtherScale> findByTenantIdOrderByCriteriaOtherIdAscScaleScoreAsc(UUID tenantId);

    Optional<RiskCriteriaOtherScale> findByTenantIdAndCriteriaOtherIdAndScaleScore(UUID tenantId, UUID criteriaOtherId, Integer scaleScore);
}
