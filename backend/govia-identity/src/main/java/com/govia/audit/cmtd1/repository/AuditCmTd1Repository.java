package com.govia.audit.cmtd1.repository;

import com.govia.audit.cmtd1.entity.AuditCmTd1;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmTd1Repository extends JpaRepository<AuditCmTd1, UUID> {
    List<AuditCmTd1> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditCmTd1> findByTenantIdAndBranchCodeAndAuditDateAndCustomerName(UUID tenantId, String branchCode, LocalDate auditDate, String customerName);
}
