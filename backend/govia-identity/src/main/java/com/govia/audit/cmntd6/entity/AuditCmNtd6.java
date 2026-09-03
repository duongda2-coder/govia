package com.govia.audit.cmntd6.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Man hinh "Danh sach chon mau User Ipcas AD, KPI CNTT" (sheet ZTC_CM_NTD6, phu luc 08B/BKS-KTNB) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cac cot kieu "List"
 * tham chieu du lieu IPCAS (ma chi nhanh/ma can bo) chua co danh muc tuong ung trong GOVIA nen duoc
 * luu la String tu do, khong FK - cung quy uoc voi AuditBranchStaff.branchCode. securityDevice co
 * Type ghi la "Number" trong spec nhung vi du ("TL02659") la chuoi alphanumeric nen luu la String -
 * loi khong nhat quan cua ban spec goc. engagementId/assignedEmployeeId/processStepSummaryId them sau
 * de loc theo Cuoc kiem toan (giong bo loc Nam ben Cham diem rui ro) + theo doi nguoi phu trach. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd6")
public class AuditCmNtd6 extends BaseEntity {

    /** "Ma cuoc kiem toan" - dong nay thuoc ve dot thuc hien kiem toan nao. */
    @Column(name = "engagement_id", columnDefinition = "uuid")
    private UUID engagementId;

    /** "Nguoi duoc phan cong" - link toi Employee (KHONG qua he thong Nhom/Phan cong cua man hinh
     * Khoi tao doan kiem toan, chi la 1 o chon nhan vien don gian tren chinh dong du lieu nay). */
    @Column(name = "assigned_employee_id", columnDefinition = "uuid")
    private UUID assignedEmployeeId;

    /** "Ma BQT_TH" - link toi AuditProcessStepSummary (danh muc "Buoc quy trinh tong hop", sheet ZTB_BQT_TH). */
    @Column(name = "process_step_summary_id", columnDefinition = "uuid")
    private UUID processStepSummaryId;

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
