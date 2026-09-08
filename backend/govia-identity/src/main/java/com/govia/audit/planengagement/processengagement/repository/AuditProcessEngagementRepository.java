package com.govia.audit.planengagement.processengagement.repository;

import com.govia.audit.planengagement.processengagement.entity.AuditProcessEngagement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditProcessEngagementRepository extends JpaRepository<AuditProcessEngagement, UUID> {
    List<AuditProcessEngagement> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<AuditProcessEngagement> findByTenantIdAndCode(UUID tenantId, String code);

    long countByTenantIdAndBusinessSegmentIdAndYear(UUID tenantId, UUID businessSegmentId, Integer year);
}
