package com.govia.audit.cmntd10.repository;

import com.govia.audit.cmntd10.entity.AuditCmNtd10;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd10Repository extends JpaRepository<AuditCmNtd10, UUID> {
    List<AuditCmNtd10> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    List<AuditCmNtd10> findByTenantIdAndEngagementIdOrderByCreatedAtAsc(UUID tenantId, UUID engagementId);

    Optional<AuditCmNtd10> findByTenantIdAndBranchCodeAndIssueDateAndCustomerNameAndAccountNumber(
            UUID tenantId, String branchCode, LocalDate issueDate, String customerName, String accountNumber);
}
