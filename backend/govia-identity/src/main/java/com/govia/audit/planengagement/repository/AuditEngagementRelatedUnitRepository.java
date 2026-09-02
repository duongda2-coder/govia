package com.govia.audit.planengagement.repository;

import com.govia.audit.planengagement.entity.AuditEngagementRelatedUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEngagementRelatedUnitRepository extends JpaRepository<AuditEngagementRelatedUnit, UUID> {
    List<AuditEngagementRelatedUnit> findByTenantIdAndAuditEngagementId(UUID tenantId, UUID auditEngagementId);

    boolean existsByTenantIdAndAuditEngagementIdAndAuditObjectUnitId(UUID tenantId, UUID auditEngagementId, UUID auditObjectUnitId);

    void deleteByAuditEngagementId(UUID auditEngagementId);
}
