package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQuantitative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskCriteriaQuantitativeRepository extends JpaRepository<RiskCriteriaQuantitative, UUID> {
    List<RiskCriteriaQuantitative> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<RiskCriteriaQuantitative> findByTenantIdAndCode(UUID tenantId, String code);
}
