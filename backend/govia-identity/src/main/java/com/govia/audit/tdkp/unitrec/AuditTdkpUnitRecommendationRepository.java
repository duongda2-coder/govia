package com.govia.audit.tdkp.unitrec;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditTdkpUnitRecommendationRepository extends JpaRepository<AuditTdkpUnitRecommendation, UUID> {
    List<AuditTdkpUnitRecommendation> findByTenantIdOrderByReportDateDescCodeDesc(UUID tenantId);

    Optional<AuditTdkpUnitRecommendation> findByTenantIdAndCode(UUID tenantId, String code);
}
