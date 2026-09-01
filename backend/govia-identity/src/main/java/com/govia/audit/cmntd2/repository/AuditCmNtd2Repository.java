package com.govia.audit.cmntd2.repository;

import com.govia.audit.cmntd2.entity.AuditCmNtd2;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd2Repository extends JpaRepository<AuditCmNtd2, UUID> {
    List<AuditCmNtd2> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditCmNtd2> findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndEntryNumber(
            UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, BigDecimal entryNumber);
}
