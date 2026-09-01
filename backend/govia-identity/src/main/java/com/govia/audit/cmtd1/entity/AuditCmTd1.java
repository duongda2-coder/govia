package com.govia.audit.cmtd1.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Man hinh "Danh sach khach hang chon mau tin dung" (sheet ZTC_CM_TD1, phu luc 02L/BKS-KTNB) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cac cot kieu "List"
 * tham chieu du lieu IPCAS (ma chi nhanh/ma KH/can bo) chua co danh muc tuong ung trong GOVIA nen
 * duoc luu la String tu do, khong FK - cung quy uoc voi AuditBranchStaff.branchCode. totalCreditBalance
 * la cot tinh toan (tong 4 cot du no), khong nhan input truc tiep tu nguoi dung. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_td1")
public class AuditCmTd1 extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "audit_date", nullable = false)
    private LocalDate auditDate;

    @Column(name = "customer_code", length = 50)
    private String customerCode;

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

    @Column(name = "debt_group", length = 20)
    private String debtGroup;

    @Column(name = "audit_scope", length = 120)
    private String auditScope;

    @Column(name = "auditor_code", length = 50)
    private String auditorCode;

    @Column(name = "sample_reason", length = 50)
    private String sampleReason;

    @Column(name = "note", length = 120)
    private String note;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
