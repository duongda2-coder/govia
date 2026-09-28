package com.govia.audit.dgcl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditDgclEvaluationLineRepository extends JpaRepository<AuditDgclEvaluationLine, UUID> {
    List<AuditDgclEvaluationLine> findByEvaluationId(UUID evaluationId);
}
