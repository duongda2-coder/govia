package com.govia.audit.tdkp.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditTdkpAssignmentRepository extends JpaRepository<AuditTdkpAssignment, UUID> {
    List<AuditTdkpAssignment> findByTenantIdAndScopeOrderByCreatedAtAsc(UUID tenantId, TdkpAssignmentScope scope);
}
