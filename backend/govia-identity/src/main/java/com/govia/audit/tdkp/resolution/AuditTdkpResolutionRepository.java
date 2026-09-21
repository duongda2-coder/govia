package com.govia.audit.tdkp.resolution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditTdkpResolutionRepository extends JpaRepository<AuditTdkpResolution, UUID> {
    List<AuditTdkpResolution> findByTenantIdOrderByIssueDateDescCodeDesc(UUID tenantId);

    Optional<AuditTdkpResolution> findByTenantIdAndCode(UUID tenantId, String code);
}
