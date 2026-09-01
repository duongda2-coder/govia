package com.govia.audit.cmntd13.repository;

import com.govia.audit.cmntd13.entity.AuditCmNtd13;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd13Repository extends JpaRepository<AuditCmNtd13, UUID> {
    List<AuditCmNtd13> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditCmNtd13> findByTenantIdAndBranchCodeAndOccurrenceDateAndMerchantAccountNumberAndBusinessRegistrationName(
            UUID tenantId, String branchCode, LocalDate occurrenceDate, String merchantAccountNumber, String businessRegistrationName);
}
