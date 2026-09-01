package com.govia.audit.cmntd14.repository;

import com.govia.audit.cmntd14.entity.AuditCmNtd14;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditCmNtd14Repository extends JpaRepository<AuditCmNtd14, UUID> {
    List<AuditCmNtd14> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<AuditCmNtd14> findByTenantIdAndBranchCodeAndAttendanceDateAndStaffCode(
            UUID tenantId, String branchCode, LocalDate attendanceDate, String staffCode);
}
