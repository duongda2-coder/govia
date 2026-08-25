package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectProcess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditObjectProcessRepository extends JpaRepository<AuditObjectProcess, UUID> {
    List<AuditObjectProcess> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditObjectProcess> findByTenantIdAndCode(UUID tenantId, String code);
}
