package com.govia.audit.cmtd2.repository;

import com.govia.audit.cmtd2.entity.AuditCmTd2;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmTd2Repository extends JpaRepository<AuditCmTd2, UUID> {
    List<AuditCmTd2> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditCmTd2> findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndCustomerName(
            UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, String customerName);
}
