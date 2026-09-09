package com.govia.audit.processstepqt.repository;

import com.govia.audit.processstepqt.entity.AuditProcessStepSummaryQt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditProcessStepSummaryQtRepository extends JpaRepository<AuditProcessStepSummaryQt, UUID> {
    List<AuditProcessStepSummaryQt> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditProcessStepSummaryQt> findByTenantIdAndCode(UUID tenantId, String code);
}
