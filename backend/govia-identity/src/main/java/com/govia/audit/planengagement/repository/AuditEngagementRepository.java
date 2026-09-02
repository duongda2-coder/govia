package com.govia.audit.planengagement.repository;

import com.govia.audit.planengagement.entity.AuditEngagement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEngagementRepository extends JpaRepository<AuditEngagement, UUID> {
    List<AuditEngagement> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<AuditEngagement> findByTenantIdAndCode(UUID tenantId, String code);

    long countByTenantIdAndAuditObjectUnitIdAndYear(UUID tenantId, UUID auditObjectUnitId, Integer year);
}
