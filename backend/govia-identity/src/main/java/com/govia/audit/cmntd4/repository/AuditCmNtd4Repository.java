package com.govia.audit.cmntd4.repository;

import com.govia.audit.cmntd4.entity.AuditCmNtd4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd4Repository extends JpaRepository<AuditCmNtd4, UUID> {
    List<AuditCmNtd4> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    List<AuditCmNtd4> findByTenantIdAndEngagementIdOrderByCreatedAtAsc(UUID tenantId, UUID engagementId);

    Optional<AuditCmNtd4> findByTenantIdAndBranchCodeAndReferenceNumberAndOpenDateAndCorebankCustomerCode(
            UUID tenantId, String branchCode, BigDecimal referenceNumber, LocalDate openDate, String corebankCustomerCode);
}
