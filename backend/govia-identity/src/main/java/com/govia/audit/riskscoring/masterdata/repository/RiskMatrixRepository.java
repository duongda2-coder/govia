package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.RiskMatrix;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskMatrixRepository extends JpaRepository<RiskMatrix, UUID> {
    List<RiskMatrix> findByTenantIdOrderByFrequencyLevelAsc(UUID tenantId);

    Optional<RiskMatrix> findByTenantIdAndFrequencyLevel(UUID tenantId, Integer frequencyLevel);
}
