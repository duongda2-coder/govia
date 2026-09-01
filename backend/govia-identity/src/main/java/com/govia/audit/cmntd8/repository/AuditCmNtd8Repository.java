package com.govia.audit.cmntd8.repository;

import com.govia.audit.cmntd8.entity.AuditCmNtd8;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd8Repository extends JpaRepository<AuditCmNtd8, UUID> {
    List<AuditCmNtd8> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditCmNtd8> findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndEntryNumber(
            UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, BigDecimal entryNumber);
}
