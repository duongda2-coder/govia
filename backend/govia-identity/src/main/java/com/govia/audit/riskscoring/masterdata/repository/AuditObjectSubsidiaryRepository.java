package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectSubsidiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditObjectSubsidiaryRepository extends JpaRepository<AuditObjectSubsidiary, UUID> {
    List<AuditObjectSubsidiary> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditObjectSubsidiary> findByTenantIdAndCode(UUID tenantId, String code);
}
