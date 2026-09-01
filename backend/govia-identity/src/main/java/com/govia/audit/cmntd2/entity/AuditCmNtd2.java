package com.govia.audit.cmntd2.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Man hinh "Danh sach chon mau giao dich ve nghiep vu HDV" (sheet ZTC_CM_NTD2, phu luc 03E/BKS-KTNB)
 * - trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cac cot kieu "List"
 * tham chieu du lieu IPCAS chua co danh muc tuong ung trong GOVIA nen duoc luu la String tu do,
 * khong FK - cung quy uoc voi AuditCmTd1. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd2")
public class AuditCmNtd2 extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "posting_user", nullable = false, length = 20)
    private String postingUser;

    /** Spec ghi Type = Number nhung "So but toan" la ma dinh danh but toan - luu BigDecimal theo dung
     * Type de nhat quan voi cac cot Number khac, khong dung lam phep tinh. */
    @Column(name = "entry_number", precision = 20, scale = 2)
    private BigDecimal entryNumber;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "account_number", length = 10)
    private String accountNumber;

    @Column(name = "book_number", length = 20)
    private String bookNumber;

    @Column(name = "transaction_type", length = 30)
    private String transactionType;

    @Column(name = "transaction_status", length = 50)
    private String transactionStatus;

    @Column(name = "audit_result", length = 120)
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
