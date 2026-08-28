package com.govia.audit.riskscoring.scoring.repository;

import com.govia.audit.riskscoring.scoring.entity.RiskTypeHO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskTypeHORepository extends JpaRepository<RiskTypeHO, UUID> {
    List<RiskTypeHO> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<RiskTypeHO> findByTenantIdAndGroupHoIdAndCode(UUID tenantId, UUID groupHoId, String code);
}
