package com.govia.audit.khkt.thang.repository;

import com.govia.audit.khkt.thang.entity.AuditKhktThang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditKhktThangRepository extends JpaRepository<AuditKhktThang, UUID> {
    List<AuditKhktThang> findByTenantIdAndYear(UUID tenantId, Integer year);

    Optional<AuditKhktThang> findByTenantIdAndYearAndAuditObjectCode(UUID tenantId, Integer year, String auditObjectCode);
}
