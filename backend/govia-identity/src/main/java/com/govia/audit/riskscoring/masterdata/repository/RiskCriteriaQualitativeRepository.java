package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQualitative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskCriteriaQualitativeRepository extends JpaRepository<RiskCriteriaQualitative, UUID> {
    List<RiskCriteriaQualitative> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<RiskCriteriaQualitative> findByTenantIdAndCode(UUID tenantId, String code);
}
