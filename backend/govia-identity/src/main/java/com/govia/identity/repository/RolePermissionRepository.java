package com.govia.identity.repository;

import com.govia.identity.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
    List<RolePermission> findByRoleId(UUID roleId);

    void deleteByRoleId(UUID roleId);
}
