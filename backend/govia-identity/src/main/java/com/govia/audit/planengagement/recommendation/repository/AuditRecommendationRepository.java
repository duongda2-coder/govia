package com.govia.audit.planengagement.recommendation.repository;

import com.govia.audit.planengagement.recommendation.entity.AuditRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditRecommendationRepository extends JpaRepository<AuditRecommendation, UUID> {
    List<AuditRecommendation> findByTenantIdAndEngagementIdOrderByCodeAsc(UUID tenantId, UUID engagementId);

    Optional<AuditRecommendation> findByTenantIdAndEngagementIdAndCode(UUID tenantId, UUID engagementId, String code);
}
