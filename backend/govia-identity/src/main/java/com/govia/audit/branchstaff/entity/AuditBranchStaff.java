package com.govia.audit.branchstaff.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Danh muc "Chuc danh can bo chi nhanh" (sheet ZTC_CN_NV, bang ZTB_CN_NV) - trong nhom "Danh muc"
 * cua "Lap ke hoach", module Kiem toan noi bo. branchCode la string thuan (cung quy uoc voi
 * AuditFinding.branchCode), khong FK toi 1 bang chi nhanh rieng vi he thong chua co bang do. */
@Getter
@Setter
@Entity
@Table(name = "audit_branch_staff")
public class AuditBranchStaff extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "staff_name", nullable = false, length = 100)
    private String staffName;

    @Column(name = "position", length = 100)
    private String position;

    /** Uu tien 1-5 (code cung, khong phai danh muc). */
    @Column(name = "priority")
    private Integer priority;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
