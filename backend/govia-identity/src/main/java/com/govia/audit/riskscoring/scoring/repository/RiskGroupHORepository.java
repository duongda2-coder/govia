package com.govia.audit.riskscoring.scoring.repository;

import com.govia.audit.riskscoring.scoring.entity.RiskGroupHO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskGroupHORepository extends JpaRepository<RiskGroupHO, UUID> {
    List<RiskGroupHO> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<RiskGroupHO> findByTenantIdAndCode(UUID tenantId, String code);
}
