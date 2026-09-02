package com.govia.audit.planengagement.repository;

import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEngagementGroupMemberRepository extends JpaRepository<AuditEngagementGroupMember, UUID> {
    List<AuditEngagementGroupMember> findByTenantIdAndGroupIdOrderByCreatedAtAsc(UUID tenantId, UUID groupId);

    List<AuditEngagementGroupMember> findByTenantIdAndGroupIdIn(UUID tenantId, List<UUID> groupIds);

    Optional<AuditEngagementGroupMember> findByTenantIdAndGroupIdAndEmployeeId(UUID tenantId, UUID groupId, UUID employeeId);

    long countByGroupId(UUID groupId);
}
