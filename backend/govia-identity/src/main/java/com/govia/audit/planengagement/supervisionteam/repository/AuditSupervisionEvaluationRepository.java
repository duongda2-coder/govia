package com.govia.audit.planengagement.supervisionteam.repository;

import com.govia.audit.planengagement.supervisionteam.entity.AuditSupervisionEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditSupervisionEvaluationRepository extends JpaRepository<AuditSupervisionEvaluation, UUID> {
    List<AuditSupervisionEvaluation> findByTenantIdAndEngagementId(UUID tenantId, UUID engagementId);

    Optional<AuditSupervisionEvaluation> findByTenantIdAndEngagementIdAndEmployeeId(UUID tenantId, UUID engagementId, UUID employeeId);
}
