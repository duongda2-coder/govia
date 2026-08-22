package com.govia.identity.repository;

import com.govia.identity.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByTenantId(UUID tenantId);

    Optional<Position> findByTenantIdAndCode(UUID tenantId, String code);

    Optional<Position> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
