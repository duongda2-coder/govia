package com.govia.audit.cmntd3.repository;

import com.govia.audit.cmntd3.entity.AuditCmNtd3;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd3Repository extends JpaRepository<AuditCmNtd3, UUID> {
    List<AuditCmNtd3> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditCmNtd3> findByTenantIdAndBranchCodeAndTransactionDateAndCustomerNameAndAccountNumber(
            UUID tenantId, String branchCode, LocalDate transactionDate, String customerName, String accountNumber);
}
