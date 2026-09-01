package com.govia.audit.cmntd1.repository;

import com.govia.audit.cmntd1.entity.AuditCmNtd1;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd1Repository extends JpaRepository<AuditCmNtd1, UUID> {
    List<AuditCmNtd1> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditCmNtd1> findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndEntryNumber(
            UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, BigDecimal entryNumber);
}
