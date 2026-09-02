package com.govia.audit.planengagement.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** "Nhom" cua 1 CKT (sheet "quan ly DKT", man hinh "Danh sach nhom cua dot kiem toan") - toi da 3
 * nhom co dinh (DIEUHANH/NTINDUNG/TINDUNG), moi nhom dung 1 truong nhom. */
@Getter
@Setter
@Entity
@Table(name = "audit_engagement_group")
public class AuditEngagementGroup extends BaseEntity {

    @Column(name = "audit_engagement_id", nullable = false, columnDefinition = "uuid")
    private UUID auditEngagementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_code", nullable = false, length = 20)
    private AuditEngagementGroupCode groupCode;

    @Column(name = "leader_employee_id", nullable = false, columnDefinition = "uuid")
    private UUID leaderEmployeeId;
}
