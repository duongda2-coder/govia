package com.govia.identity.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * 1 quy tac trong "ma tran phe duyet": neu orgUnitId null thi la quy tac MAC DINH (fallback) cho ca
 * tenant, khac null thi chi ap dung cho don vi to chuc do. Dung boi EmployeeApprovalService de sinh
 * dây phe duyet dong (thay vi so cap co dinh) - xem finalApprovalLevel/requireFinalSuperAdminStep.
 */
@Getter
@Setter
@Entity
@Table(name = "approval_matrix_rule")
public class ApprovalMatrixRule extends BaseEntity {

    /** Domain nghiep vu ap dung quy tac nay (vd "EMPLOYEE", "AUDIT_WORKITEM") - cho phep 1 bang
     * dung chung cho nhieu loai phe duyet dong khac nhau, moi domain co khong gian scope
     * (org_unit_id) rieng. Mac dinh "EMPLOYEE" de tuong thich nguoc voi du lieu da co truoc khi
     * them cot nay. */
    @Column(name = "domain", nullable = false, length = 40)
    private String domain = "EMPLOYEE";

    /** Null = quy tac mac dinh (fallback) cho toan tenant, khong rieng don vi nao. */
    @Column(name = "org_unit_id", columnDefinition = "uuid")
    private UUID orgUnitId;

    /** Di nguoc chuoi quan ly toi khi gap nguoi co rank_level >= gia tri nay thi dung lai. */
    @Enumerated(EnumType.STRING)
    @Column(name = "final_approval_level", nullable = false, length = 10)
    private EmployeeRankLevel finalApprovalLevel;

    /** Co them 1 buoc Super Admin sau nguoi duyet cuoi cua "day" hay khong. */
    @Column(name = "require_final_super_admin_step", nullable = false)
    private boolean requireFinalSuperAdminStep = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
