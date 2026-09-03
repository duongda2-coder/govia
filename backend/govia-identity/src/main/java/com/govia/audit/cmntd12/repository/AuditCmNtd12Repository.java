package com.govia.audit.cmntd12.repository;

import com.govia.audit.cmntd12.entity.AuditCmNtd12;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd12Repository extends JpaRepository<AuditCmNtd12, UUID> {
    List<AuditCmNtd12> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    List<AuditCmNtd12> findByTenantIdAndEngagementIdOrderByCreatedAtAsc(UUID tenantId, UUID engagementId);

    Optional<AuditCmNtd12> findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndEntryNumber(
            UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, BigDecimal entryNumber);
}
