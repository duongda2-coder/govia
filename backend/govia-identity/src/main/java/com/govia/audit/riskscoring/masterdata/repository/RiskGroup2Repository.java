package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.RiskGroup2;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskGroup2Repository extends JpaRepository<RiskGroup2, UUID> {
    List<RiskGroup2> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<RiskGroup2> findByTenantIdAndCode(UUID tenantId, String code);
}
