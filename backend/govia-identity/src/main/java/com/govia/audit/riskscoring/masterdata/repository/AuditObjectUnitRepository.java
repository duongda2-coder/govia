package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditObjectUnitRepository extends JpaRepository<AuditObjectUnit, UUID> {
    List<AuditObjectUnit> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditObjectUnit> findByTenantIdAndCode(UUID tenantId, String code);
}
