package com.govia.identity.maildemo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailDemoSettingRepository extends JpaRepository<EmailDemoSetting, UUID> {

    Optional<EmailDemoSetting> findByTenantId(UUID tenantId);
}
