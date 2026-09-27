package com.govia.audit.planengagement.ttss.repository;

import com.govia.audit.planengagement.ttss.entity.AuditTtssRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuditTtssRecordRepository extends JpaRepository<AuditTtssRecord, UUID> {
    List<AuditTtssRecord> findByTenantId(UUID tenantId);

    List<AuditTtssRecord> findByTenantIdAndEngagementIdOrderByCreatedAtAsc(UUID tenantId, UUID engagementId);

    List<AuditTtssRecord> findByTenantIdAndEngagementIdIn(UUID tenantId, List<UUID> engagementIds);

    @Query("select count(r) > 0 from AuditTtssRecord r join r.teamRecommendationIds rid where r.tenantId = :tenantId and rid = :recommendationId")
    boolean existsLinkedToRecommendation(@Param("tenantId") UUID tenantId, @Param("recommendationId") UUID recommendationId);
}
