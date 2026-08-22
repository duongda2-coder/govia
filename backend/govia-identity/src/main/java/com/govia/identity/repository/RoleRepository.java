package com.govia.identity.repository;

import com.govia.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByTenantIdAndCode(UUID tenantId, String code);

    List<Role> findByTenantId(UUID tenantId);
}
