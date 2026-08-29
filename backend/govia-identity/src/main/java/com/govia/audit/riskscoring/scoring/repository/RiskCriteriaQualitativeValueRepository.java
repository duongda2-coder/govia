package com.govia.audit.riskscoring.scoring.repository;

import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaQualitativeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskCriteriaQualitativeValueRepository extends JpaRepository<RiskCriteriaQualitativeValue, UUID> {
    List<RiskCriteriaQualitativeValue> findByTenantIdAndYearOrderByBranchCodeAsc(UUID tenantId, Integer year);

    List<RiskCriteriaQualitativeValue> findByTenantIdAndYearBetween(UUID tenantId, Integer fromYear, Integer toYear);

    Optional<RiskCriteriaQualitativeValue> findByTenantIdAndCriteriaIdAndBranchCodeAndYear(
            UUID tenantId, UUID criteriaId, String branchCode, Integer year);
}
