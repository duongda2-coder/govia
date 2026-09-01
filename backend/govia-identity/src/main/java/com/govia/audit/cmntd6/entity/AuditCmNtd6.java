package com.govia.audit.cmntd6.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Man hinh "Danh sach chon mau User Ipcas AD, KPI CNTT" (sheet ZTC_CM_NTD6, phu luc 08B/BKS-KTNB) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cac cot kieu "List"
 * tham chieu du lieu IPCAS (ma chi nhanh/ma can bo) chua co danh muc tuong ung trong GOVIA nen duoc
 * luu la String tu do, khong FK - cung quy uoc voi AuditBranchStaff.branchCode. securityDevice co
 * Type ghi la "Number" trong spec nhung vi du ("TL02659") la chuoi alphanumeric nen luu la String -
 * loi khong nhat quan cua ban spec goc. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd6")
public class AuditCmNtd6 extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "staff_code", length = 50)
    private String staffCode;

    @Column(name = "staff_name", nullable = false, length = 100)
    private String staffName;

    @Column(name = "ipcas_user", nullable = false, length = 20)
    private String ipcasUser;

    @Column(name = "ad_user", length = 20)
    private String adUser;

    /** Spec ghi Type = Number nhung vi du "TL02659" la alphanumeric -> luu String. */
    @Column(name = "security_device", length = 20)
    private String securityDevice;

    @Column(name = "sample_reason", length = 50)
    private String sampleReason;

    @Column(name = "sample_code", length = 20)
    private String sampleCode;

    @Column(name = "audit_result", length = 120)
    private String auditResult;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
