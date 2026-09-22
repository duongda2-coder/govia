package com.govia.audit.phbc.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuditReportIssuanceRecommendationRepository extends JpaRepository<AuditReportIssuanceRecommendation, UUID> {
    List<AuditReportIssuanceRecommendation> findByTenantIdAndReportIssuanceIdOrderByCreatedAtAsc(UUID tenantId, UUID reportIssuanceId);

    List<AuditReportIssuanceRecommendation> findByTenantIdAndReportIssuanceIdIn(UUID tenantId, Collection<UUID> reportIssuanceIds);

    void deleteByTenantIdAndReportIssuanceId(UUID tenantId, UUID reportIssuanceId);
}
