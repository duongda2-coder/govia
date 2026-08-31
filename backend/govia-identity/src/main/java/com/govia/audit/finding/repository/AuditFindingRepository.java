package com.govia.audit.finding.repository;

import com.govia.audit.finding.entity.AuditFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditFindingRepository extends JpaRepository<AuditFinding, UUID> {
    List<AuditFinding> findByTenantIdOrderByDetectedDateDesc(UUID tenantId);
}
