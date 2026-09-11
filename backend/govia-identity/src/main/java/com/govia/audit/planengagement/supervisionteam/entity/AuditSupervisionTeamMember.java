package com.govia.audit.planengagement.supervisionteam.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Thanh vien "To giam sat" cua 1 CKT (sheet "To giam sat" cua Tao CKT (4).xlsx) - chi nguoi tao ra
 * CKT do moi duoc chon/thay doi danh sach nay, xem AuditSupervisionTeamService.saveTeam(). */
@Getter
@Setter
@Entity
@Table(name = "audit_supervision_team_member")
public class AuditSupervisionTeamMember extends BaseEntity {

    @Column(name = "engagement_id", nullable = false, columnDefinition = "uuid")
    private UUID engagementId;

    @Column(name = "employee_id", nullable = false, columnDefinition = "uuid")
    private UUID employeeId;
}
