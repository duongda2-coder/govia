package com.govia.audit.khkt.th.repository;

import com.govia.audit.khkt.th.entity.AuditKhktThCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditKhktThCandidateRepository extends JpaRepository<AuditKhktThCandidate, UUID> {
    List<AuditKhktThCandidate> findByTenantIdAndYearOrderByAuditObjectCodeAsc(UUID tenantId, Integer year);

    Optional<AuditKhktThCandidate> findByTenantIdAndYearAndAuditObjectCode(UUID tenantId, Integer year, String auditObjectCode);
}
