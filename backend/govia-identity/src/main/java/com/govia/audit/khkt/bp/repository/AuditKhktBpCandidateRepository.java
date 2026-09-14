package com.govia.audit.khkt.bp.repository;

import com.govia.audit.khkt.bp.entity.AuditKhktBpCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditKhktBpCandidateRepository extends JpaRepository<AuditKhktBpCandidate, UUID> {
    List<AuditKhktBpCandidate> findByTenantIdAndDepartmentIdAndYearOrderByAuditObjectCodeAsc(UUID tenantId, UUID departmentId, Integer year);

    Optional<AuditKhktBpCandidate> findByTenantIdAndDepartmentIdAndYearAndAuditObjectCode(
            UUID tenantId, UUID departmentId, Integer year, String auditObjectCode);
}
