package com.govia.audit.planengagement.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** "Nhom" cua 1 CKT (sheet "quan ly DKT", man hinh "Danh sach nhom cua dot kiem toan"). Voi CKT
 * thuong: toi da 3 nhom co dinh (DIEUHANH/NTINDUNG/TINDUNG). Voi CKT quy trinh (co processEngagementId):
 * chi 1 nhom, ma nhom = ma nghiep vu (BUSINESS_SEGMENT) cua CKT quy trinh cha, vd "AM" - xem
 * AuditEngagementTeamService.validGroupCodes(). Moi nhom dung 1 truong nhom. */
@Getter
@Setter
@Entity
@Table(name = "audit_engagement_group")
public class AuditEngagementGroup extends BaseEntity {

    @Column(name = "audit_engagement_id", nullable = false, columnDefinition = "uuid")
    private UUID auditEngagementId;

    @Column(name = "group_code", nullable = false, length = 20)
    private String groupCode;

    @Column(name = "leader_employee_id", nullable = false, columnDefinition = "uuid")
    private UUID leaderEmployeeId;
}
