package com.govia.identity.repository;

import com.govia.identity.entity.OrganizationUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, UUID> {
    List<OrganizationUnit> findByTenantId(UUID tenantId);

    Optional<OrganizationUnit> findByTenantIdAndCode(UUID tenantId, String code);

    Optional<OrganizationUnit> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    boolean existsByManagerEmployeeId(UUID managerEmployeeId);
}
