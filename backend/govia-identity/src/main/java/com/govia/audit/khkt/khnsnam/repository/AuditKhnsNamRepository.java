package com.govia.audit.khkt.khnsnam.repository;

import com.govia.audit.khkt.khnsnam.entity.AuditKhnsNam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditKhnsNamRepository extends JpaRepository<AuditKhnsNam, UUID> {
    List<AuditKhnsNam> findByTenantIdAndYear(UUID tenantId, Integer year);

    Optional<AuditKhnsNam> findByTenantIdAndYearAndEmployeeId(UUID tenantId, Integer year, UUID employeeId);
}
