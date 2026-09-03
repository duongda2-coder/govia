package com.govia.audit.employeecapability.repository;

import com.govia.audit.employeecapability.entity.AuditEmployeeCapability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEmployeeCapabilityRepository extends JpaRepository<AuditEmployeeCapability, UUID> {
    List<AuditEmployeeCapability> findByTenantId(UUID tenantId);

    Optional<AuditEmployeeCapability> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId);
}
