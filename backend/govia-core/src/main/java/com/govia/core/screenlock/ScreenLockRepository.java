package com.govia.core.screenlock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScreenLockRepository extends JpaRepository<ScreenLock, UUID> {

    Optional<ScreenLock> findByTenantIdAndScreenKey(UUID tenantId, String screenKey);

    void deleteByTenantIdAndScreenKey(UUID tenantId, String screenKey);
}
