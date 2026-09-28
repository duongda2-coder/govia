package com.govia.audit.dgcl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditDgclEvaluationRepository extends JpaRepository<AuditDgclEvaluation, UUID> {
    List<AuditDgclEvaluation> findByTenantIdAndEngagementId(UUID tenantId, UUID engagementId);

    Optional<AuditDgclEvaluation> findByTenantIdAndEngagementIdAndSubjectKeyAndAppendix(UUID tenantId, UUID engagementId, String subjectKey,
                                                                                    DgclAppendix appendix);
}
