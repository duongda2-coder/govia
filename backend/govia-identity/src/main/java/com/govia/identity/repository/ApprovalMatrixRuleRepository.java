package com.govia.identity.repository;

import com.govia.identity.entity.ApprovalMatrixRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalMatrixRuleRepository extends JpaRepository<ApprovalMatrixRule, UUID> {
    List<ApprovalMatrixRule> findByTenantId(UUID tenantId);

    Optional<ApprovalMatrixRule> findByTenantIdAndOrgUnitId(UUID tenantId, UUID orgUnitId);

    Optional<ApprovalMatrixRule> findByTenantIdAndOrgUnitIdIsNull(UUID tenantId);
}
