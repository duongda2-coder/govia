package com.govia.audit.tdkp.ceo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditTdkpCeoRecommendationRepository extends JpaRepository<AuditTdkpCeoRecommendation, UUID> {
    List<AuditTdkpCeoRecommendation> findByTenantIdAndScopeOrderByReportDateDescCreatedAtDesc(UUID tenantId, TdkpCeoScope scope);
}
