package com.govia.audit.processstepqt.repository;

import com.govia.audit.processstepqt.entity.AuditProcessStepDetailQt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditProcessStepDetailQtRepository extends JpaRepository<AuditProcessStepDetailQt, UUID> {
    List<AuditProcessStepDetailQt> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditProcessStepDetailQt> findByTenantIdAndCodeAndApplicableYear(UUID tenantId, String code, Integer applicableYear);
}
