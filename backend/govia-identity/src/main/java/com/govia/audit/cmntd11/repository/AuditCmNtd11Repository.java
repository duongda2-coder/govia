package com.govia.audit.cmntd11.repository;

import com.govia.audit.cmntd11.entity.AuditCmNtd11;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd11Repository extends JpaRepository<AuditCmNtd11, UUID> {
    List<AuditCmNtd11> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    List<AuditCmNtd11> findByTenantIdAndEngagementIdOrderByCreatedAtAsc(UUID tenantId, UUID engagementId);

    Optional<AuditCmNtd11> findByTenantIdAndBranchCodeAndReferenceNumberAndCustomerCodeAndTransactionDate(
            UUID tenantId, String branchCode, BigDecimal referenceNumber, String customerCode, LocalDate transactionDate);
}
