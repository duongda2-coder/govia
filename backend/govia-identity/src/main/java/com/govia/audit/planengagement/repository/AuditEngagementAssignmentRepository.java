package com.govia.audit.planengagement.repository;

import com.govia.audit.planengagement.entity.AuditEngagementAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEngagementAssignmentRepository extends JpaRepository<AuditEngagementAssignment, UUID> {
    List<AuditEngagementAssignment> findByTenantIdAndGroupMemberIdOrderByCreatedAtAsc(UUID tenantId, UUID groupMemberId);

    List<AuditEngagementAssignment> findByTenantIdAndGroupMemberIdIn(UUID tenantId, List<UUID> groupMemberIds);

    boolean existsByGroupMemberIdAndWorkItemId(UUID groupMemberId, UUID workItemId);

    void deleteByGroupMemberId(UUID groupMemberId);
}
