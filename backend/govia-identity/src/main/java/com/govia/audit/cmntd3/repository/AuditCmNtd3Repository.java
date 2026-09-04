package com.govia.audit.cmntd3.repository;

import com.govia.audit.cmntd3.entity.AuditCmNtd3;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd3Repository extends JpaRepository<AuditCmNtd3, UUID> {
    List<AuditCmNtd3> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    List<AuditCmNtd3> findByTenantIdAndEngagementIdOrderByCreatedAtAsc(UUID tenantId, UUID engagementId);

    Optional<AuditCmNtd3> findByTenantIdAndBranchCodeAndCustomerNameAndCorebankCustomerCode(
            UUID tenantId, String branchCode, String customerName, String corebankCustomerCode);
}
