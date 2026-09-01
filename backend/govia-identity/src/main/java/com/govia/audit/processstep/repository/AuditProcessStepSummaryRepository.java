package com.govia.audit.processstep.repository;

import com.govia.audit.processstep.entity.AuditProcessStepSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditProcessStepSummaryRepository extends JpaRepository<AuditProcessStepSummary, UUID> {
    List<AuditProcessStepSummary> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditProcessStepSummary> findByTenantIdAndCode(UUID tenantId, String code);
}
