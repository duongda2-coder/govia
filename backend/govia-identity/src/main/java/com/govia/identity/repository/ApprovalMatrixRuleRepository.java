package com.govia.identity.repository;

import com.govia.identity.entity.ApprovalMatrixRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalMatrixRuleRepository extends JpaRepository<ApprovalMatrixRule, UUID> {
    List<ApprovalMatrixRule> findByTenantIdAndDomain(UUID tenantId, String domain);

    Optional<ApprovalMatrixRule> findByTenantIdAndDomainAndOrgUnitId(UUID tenantId, String domain, UUID orgUnitId);

    Optional<ApprovalMatrixRule> findByTenantIdAndDomainAndOrgUnitIdIsNull(UUID tenantId, String domain);
}
