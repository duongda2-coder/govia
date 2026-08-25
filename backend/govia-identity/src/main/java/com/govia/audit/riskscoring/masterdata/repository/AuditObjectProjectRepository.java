package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditObjectProjectRepository extends JpaRepository<AuditObjectProject, UUID> {
    List<AuditObjectProject> findByTenantIdOrderByCodeAsc(UUID tenantId);

    Optional<AuditObjectProject> findByTenantIdAndCode(UUID tenantId, String code);
}
