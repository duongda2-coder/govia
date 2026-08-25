package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditObjectCategoryRepository extends JpaRepository<AuditObjectCategory, UUID> {
    List<AuditObjectCategory> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditObjectCategory> findByTenantIdAndCode(UUID tenantId, String code);
}
