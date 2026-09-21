package com.govia.audit.tdkp.branch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuditTdkpBranchDefectRepository extends JpaRepository<AuditTdkpBranchDefect, UUID> {
    List<AuditTdkpBranchDefect> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    List<AuditTdkpBranchDefect> findByTenantIdAndBranchRecommendationIdOrderByCreatedAtAsc(UUID tenantId, UUID branchRecommendationId);

    List<AuditTdkpBranchDefect> findByTenantIdAndBranchRecommendationIdIn(UUID tenantId, Collection<UUID> branchRecommendationIds);

    boolean existsByTenantIdAndSourceTtssId(UUID tenantId, UUID sourceTtssId);

    void deleteByTenantIdAndBranchRecommendationId(UUID tenantId, UUID branchRecommendationId);
}
