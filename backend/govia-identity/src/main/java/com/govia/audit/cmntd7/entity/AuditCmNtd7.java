package com.govia.audit.cmntd7.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Man hinh "Danh sach chon mau ho so cong trinh XDCB" (sheet ZTC_CM_NTD7, phu luc 09B/BKS-KTNB) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cot branchCode tham
 * chieu du lieu IPCAS (ma chi nhanh) chua co danh muc tuong ung trong GOVIA nen duoc luu la String
 * tu do, khong FK - cung quy uoc voi AuditBranchStaff.branchCode. engagementId/assignedEmployeeId/
 * processStepSummaryId them sau de loc theo Cuoc kiem toan (giong bo loc Nam ben Cham diem rui ro)
 * + theo doi nguoi phu trach. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd7")
public class AuditCmNtd7 extends BaseEntity {

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
