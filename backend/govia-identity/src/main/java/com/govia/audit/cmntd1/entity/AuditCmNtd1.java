package com.govia.audit.cmntd1.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Man hinh "Danh sach cac but toan chon mau TCKT" (sheet ZTC_CM_NTD1, phu luc 04C/BKS-KTNB) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cac cot kieu "List"
 * tham chieu du lieu IPCAS chua co danh muc tuong ung trong GOVIA nen duoc luu la String tu do,
 * khong FK - cung quy uoc voi AuditCmTd1. auditResult, recommendationType tam thoi nhap tay (spec
 * ghi chu se link qua Ziaexe sau). */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd1")
public class AuditCmNtd1 extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "posting_user", nullable = false, length = 20)
    private String postingUser;

    /** Spec ghi Type = Number nhung "So but toan" la ma dinh danh but toan - luu BigDecimal theo dung
     * Type de nhat quan voi cac cot Number khac, khong dung lam phep tinh. */
    @Column(name = "entry_number", precision = 20, scale = 2)
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

    /** Spec ghi Type = "number" nhung vi du la text "ABC" - noi dung mo ta, luu String theo vi du. */
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

    @Column(name = "work_type", length = 50)
    private String workType;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
