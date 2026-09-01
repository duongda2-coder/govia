package com.govia.audit.cmntd7.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Man hinh "Danh sach chon mau ho so cong trinh XDCB" (sheet ZTC_CM_NTD7, phu luc 09B/BKS-KTNB) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cot branchCode tham
 * chieu du lieu IPCAS (ma chi nhanh) chua co danh muc tuong ung trong GOVIA nen duoc luu la String
 * tu do, khong FK - cung quy uoc voi AuditBranchStaff.branchCode. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd7")
public class AuditCmNtd7 extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "construction_code", nullable = false, length = 10)
    private String constructionCode;

    @Column(name = "construction_name", length = 50)
    private String constructionName;

    @Column(name = "content", length = 120)
    private String content;

    @Column(name = "document_type", length = 20)
    private String documentType;

    @Column(name = "completeness_assessment", length = 120)
    private String completenessAssessment;

    @Column(name = "assessment", length = 250)
    private String assessment;

    @Column(name = "audit_result", length = 120)
    private String auditResult;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
