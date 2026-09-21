package com.govia.audit.tdkp.branch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditTdkpBranchRecommendationRepository extends JpaRepository<AuditTdkpBranchRecommendation, UUID> {
    List<AuditTdkpBranchRecommendation> findByTenantIdOrderByManagementCodeAsc(UUID tenantId);

    Optional<AuditTdkpBranchRecommendation> findByTenantIdAndManagementCode(UUID tenantId, String managementCode);

    Optional<AuditTdkpBranchRecommendation> findByTenantIdAndEngagementIdAndSourceRecommendationId(UUID tenantId, UUID engagementId, UUID sourceRecommendationId);
}
