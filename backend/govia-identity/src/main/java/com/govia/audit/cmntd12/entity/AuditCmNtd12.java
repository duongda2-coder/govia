package com.govia.audit.cmntd12.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Man hinh "Danh cac but toan chon mau TCKT" (sheet ZTC_CM_NTD12, phu luc 07F/BKS-KTNB) - trong
 * nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cac cot kieu "List" tham
 * chieu du lieu IPCAS (ma chi nhanh/trang thai giao dich/ly do chon mau) chua co danh muc tuong
 * ung trong GOVIA nen duoc luu la String tu do, khong FK - cung quy uoc voi
 * AuditBranchStaff.branchCode. Cot "Noi dung" ghi Type = "number" trong sheet nhung vi du la text
 * ("ABC") - trai voi Type, nen duoc luu la String theo vi du (xem quy tac spec-inconsistency).
 * Cot "Kết quả kiểm toán" co Type "Link" (link tu ziaexe > Thuc hien > Phat hien) - van luu String
 * tu do, khong FK. engagementId/assignedEmployeeId/processStepSummaryId them sau de loc theo Cuoc
 * kiem toan (giong bo loc Nam ben Cham diem rui ro) + theo doi nguoi phu trach. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd12")
public class AuditCmNtd12 extends BaseEntity {

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

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "posting_user", nullable = false, length = 20)
    private String postingUser;

    @Column(name = "entry_number", nullable = false, precision = 20, scale = 2)
    private BigDecimal entryNumber;

    @Column(name = "debit_amount", precision = 20, scale = 2)
    private BigDecimal debitAmount;

    @Column(name = "credit_amount", precision = 20, scale = 2)
    private BigDecimal creditAmount;

    @Column(name = "transaction_status", length = 50)
    private String transactionStatus;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "account_number", length = 10)
    private String accountNumber;

    /** Type trong sheet ghi "number" nhung vi du la text ("ABC") - trai voi Type, luu String theo vi du. */
    @Column(name = "content", length = 200)
    private String content;

    @Column(name = "sample_reason", length = 50)
    private String sampleReason;

    @Column(name = "audit_result", length = 200)
    private String auditResult;

    @Column(name = "recommendation_type", length = 120)
    private String recommendationType;

    @Column(name = "transaction_staff", length = 120)
    private String transactionStaff;

    @Column(name = "control_user", length = 120)
    private String controlUser;

    @Column(name = "control_staff", length = 120)
    private String controlStaff;

    @Column(name = "control_staff_title", length = 120)
    private String controlStaffTitle;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
