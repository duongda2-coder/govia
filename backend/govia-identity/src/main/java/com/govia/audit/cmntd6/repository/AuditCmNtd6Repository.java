package com.govia.audit.cmntd6.repository;

import com.govia.audit.cmntd6.entity.AuditCmNtd6;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd6Repository extends JpaRepository<AuditCmNtd6, UUID> {
    List<AuditCmNtd6> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    List<AuditCmNtd6> findByTenantIdAndEngagementIdOrderByCreatedAtAsc(UUID tenantId, UUID engagementId);

    Optional<AuditCmNtd6> findByTenantIdAndBranchCodeAndStaffNameAndIpcasUser(
            UUID tenantId, String branchCode, String staffName, String ipcasUser);
}
