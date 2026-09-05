package com.govia.audit.cmtd1.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Man hinh "Danh sach khach hang chon mau tin dung" (sheet ZTC_CM_TD1, phu luc 02L/BKS-KTNB) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cac cot kieu "List"
 * tham chieu du lieu IPCAS (ma chi nhanh/ma KH/can bo) chua co danh muc tuong ung trong GOVIA nen
 * duoc luu la String tu do, khong FK - cung quy uoc voi AuditBranchStaff.branchCode. totalCreditBalance
 * la cot tinh toan (tong 4 cot du no), khong nhan input truc tiep tu nguoi dung. engagementId/
 * assignedEmployeeId/processStepSummaryId them sau de loc theo Cuoc kiem toan (giong bo loc Nam ben
 * Cham diem rui ro) + theo doi nguoi phu trach. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_td1")
public class AuditCmTd1 extends BaseEntity {

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

    @Column(name = "audit_date", nullable = false)
    private LocalDate auditDate;

    @Column(name = "customer_code", length = 50)
    private String customerCode;

    /** "User loc mau" - user thuc hien loc/chon mau tren he thong loc mau, KHONG phai can bo kiem toan. */
    @Column(name = "sample_filter_user", length = 20)
    private String sampleFilterUser;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "approved_amount", precision = 20, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "loan_purpose", length = 60)
    private String loanPurpose;

    @Column(name = "description", length = 120)
    private String description;

    @Column(name = "on_balance_debt", precision = 20, scale = 2)
    private BigDecimal onBalanceDebt;

    @Column(name = "guarantee_balance", precision = 20, scale = 2)
    private BigDecimal guaranteeBalance;

    @Column(name = "risk_classified_debt", precision = 20, scale = 2)
    private BigDecimal riskClassifiedDebt;

    @Column(name = "vamc_sold_debt", precision = 20, scale = 2)
    private BigDecimal vamcSoldDebt;

    /** Tong 4 cot du no (H+I+J+K) - he thong tu tinh khi create/update, xem AuditCmTd1Service. */
    @Column(name = "total_credit_balance", precision = 20, scale = 2)
    private BigDecimal totalCreditBalance;

    @Column(name = "debt_group", length = 100)
    private String debtGroup;

    @Column(name = "audit_scope", length = 120)
    private String auditScope;

    @Column(name = "auditor_code", length = 50)
    private String auditorCode;

    @Column(name = "sample_reason", length = 1000)
    private String sampleReason;

    @Column(name = "note", length = 120)
    private String note;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
