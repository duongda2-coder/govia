package com.govia.audit.planengagement.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** "Phan cong nghiep vu cho thanh vien" - 1 cong viec kiem toan (AuditWorkItem) duoc giao cho 1
 * thanh vien nhom. Duoc tao tu dong khi them thanh vien (theo nghiep vu 1/2/3 cua thanh vien do),
 * hoac them thu cong qua nut "Chon cong viec". */
@Getter
@Setter
@Entity
@Table(name = "audit_engagement_assignment")
public class AuditEngagementAssignment extends BaseEntity {

    @Column(name = "group_member_id", nullable = false, columnDefinition = "uuid")
    private UUID groupMemberId;

    @Column(name = "work_item_id", nullable = false, columnDefinition = "uuid")
    private UUID workItemId;
}
