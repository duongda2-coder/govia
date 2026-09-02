package com.govia.audit.planengagement.repository;

import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEngagementGroupRepository extends JpaRepository<AuditEngagementGroup, UUID> {
    List<AuditEngagementGroup> findByTenantIdAndAuditEngagementIdOrderByGroupCodeAsc(UUID tenantId, UUID auditEngagementId);

    Optional<AuditEngagementGroup> findByTenantIdAndAuditEngagementIdAndGroupCode(UUID tenantId, UUID auditEngagementId, AuditEngagementGroupCode groupCode);
}
