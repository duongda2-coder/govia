package com.govia.identity.repository;

import com.govia.identity.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByTenantIdAndUsername(UUID tenantId, String username);

    Optional<UserAccount> findByEmployeeId(UUID employeeId);

    boolean existsByEmployeeId(UUID employeeId);

    List<UserAccount> findByEmployeeIdIn(Collection<UUID> employeeIds);

    List<UserAccount> findByTenantId(UUID tenantId);
}
