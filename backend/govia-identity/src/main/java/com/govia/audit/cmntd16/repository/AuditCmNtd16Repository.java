package com.govia.audit.cmntd16.repository;

import com.govia.audit.cmntd16.entity.AuditCmNtd16;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd16Repository extends JpaRepository<AuditCmNtd16, UUID> {
    List<AuditCmNtd16> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    List<AuditCmNtd16> findByTenantIdAndEngagementIdOrderByCreatedAtAsc(UUID tenantId, UUID engagementId);

    Optional<AuditCmNtd16> findByTenantIdAndBranchCodeAndTransactionDateAndPostingUserAndEntryNumber(
            UUID tenantId, String branchCode, LocalDate transactionDate, String postingUser, BigDecimal entryNumber);
}
