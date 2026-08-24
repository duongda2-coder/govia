package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.RiskFrequencyCoefficient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskFrequencyCoefficientRepository extends JpaRepository<RiskFrequencyCoefficient, UUID> {
    List<RiskFrequencyCoefficient> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<RiskFrequencyCoefficient> findByTenantIdAndCode(UUID tenantId, String code);
}
