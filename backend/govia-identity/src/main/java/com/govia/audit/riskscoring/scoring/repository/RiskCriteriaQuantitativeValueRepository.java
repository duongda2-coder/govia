package com.govia.audit.riskscoring.scoring.repository;

import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaQuantitativeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskCriteriaQuantitativeValueRepository extends JpaRepository<RiskCriteriaQuantitativeValue, UUID> {
    List<RiskCriteriaQuantitativeValue> findByTenantIdAndYearOrderByBranchCodeAsc(UUID tenantId, Integer year);

    List<RiskCriteriaQuantitativeValue> findByTenantIdAndBranchCodeAndYear(UUID tenantId, String branchCode, Integer year);

    Optional<RiskCriteriaQuantitativeValue> findByTenantIdAndCriteriaIdAndBranchCodeAndYear(
            UUID tenantId, UUID criteriaId, String branchCode, Integer year);
}
