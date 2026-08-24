package com.govia.audit.riskscoring.masterdata.repository;

import com.govia.audit.riskscoring.masterdata.entity.RiskUserAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskUserAssignmentRepository extends JpaRepository<RiskUserAssignment, UUID> {
    List<RiskUserAssignment> findByTenantIdOrderByUsernameAsc(UUID tenantId);

    Optional<RiskUserAssignment> findByTenantIdAndUsernameAndCriteriaIdAndBranchCode(
            UUID tenantId, String username, UUID criteriaId, String branchCode);
}
