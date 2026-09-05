package com.govia.audit.planengagement.ttss.repository;

import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditTtssRecordRepository extends JpaRepository<AuditTtssRecord, UUID> {
    List<AuditTtssRecord> findByTenantIdAndEngagementIdOrderByCreatedAtAsc(UUID tenantId, UUID engagementId);
}
