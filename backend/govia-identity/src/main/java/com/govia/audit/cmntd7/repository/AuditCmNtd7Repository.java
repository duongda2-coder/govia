package com.govia.audit.cmntd7.repository;

import com.govia.audit.cmntd7.entity.AuditCmNtd7;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd7Repository extends JpaRepository<AuditCmNtd7, UUID> {
    List<AuditCmNtd7> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditCmNtd7> findByTenantIdAndBranchCodeAndConstructionCode(UUID tenantId, String branchCode, String constructionCode);
}
