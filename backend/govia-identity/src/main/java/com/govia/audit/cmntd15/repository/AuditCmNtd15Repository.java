package com.govia.audit.cmntd15.repository;

import com.govia.audit.cmntd15.entity.AuditCmNtd15;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd15Repository extends JpaRepository<AuditCmNtd15, UUID> {
    List<AuditCmNtd15> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    List<AuditCmNtd15> findByTenantIdAndEngagementIdOrderByCreatedAtAsc(UUID tenantId, UUID engagementId);

    Optional<AuditCmNtd15> findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndEntryNumber(
            UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, BigDecimal entryNumber);
}
