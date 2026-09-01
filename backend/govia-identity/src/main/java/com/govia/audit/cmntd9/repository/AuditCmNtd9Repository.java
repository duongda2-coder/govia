package com.govia.audit.cmntd9.repository;

import com.govia.audit.cmntd9.entity.AuditCmNtd9;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd9Repository extends JpaRepository<AuditCmNtd9, UUID> {
    List<AuditCmNtd9> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditCmNtd9> findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndCustomerName(
            UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, String customerName);
}
