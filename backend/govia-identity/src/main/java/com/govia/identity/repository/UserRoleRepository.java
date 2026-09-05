package com.govia.identity.repository;

import com.govia.identity.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserId(UUID userId);

    List<UserRole> findByRoleId(UUID roleId);

    void deleteByUserId(UUID userId);

    boolean existsByRoleId(UUID roleId);

    long countByRoleId(UUID roleId);
}
