package com.govia.identity.repository;

import com.govia.identity.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByTenantIdAndEmployeeCode(UUID tenantId, String employeeCode);

    boolean existsByManagerId(UUID managerId);

    boolean existsByOrgUnitId(UUID orgUnitId);
}
