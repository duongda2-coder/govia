package com.govia.audit.cmntd10.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Man hinh "Ket qua kiem toan ho so phat hanh the" (sheet ZTC_CM_NTD10, phu luc 07B/BKS-KTNB) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cac cot kieu "List"
 * tham chieu du lieu IPCAS (ma chi nhanh/ma KH/can bo) chua co danh muc tuong ung trong GOVIA nen
 * duoc luu la String tu do, khong FK - cung quy uoc voi AuditBranchStaff.branchCode. issuanceType
 * va issuanceOccurrence la list cung 2 gia tri co dinh theo cot "Logic" cua sheet, nhung van luu
 * String (khong FK) - UI gioi han bang Select, xem AuditCmNtd10Request. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd10")
public class AuditCmNtd10 extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "customer_code", length = 50)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "card_tier", length = 20)
    private String cardTier;

    @Column(name = "issuing_user", length = 20)
    private String issuingUser;

    @Column(name = "issuance_fee", precision = 20, scale = 2)
    private BigDecimal issuanceFee;

    /** List cung: "Phát hành nhanh" hoac "Phát hành thường" (khong FK, gioi han o UI). */
    @Column(name = "issuance_type", length = 20)
    private String issuanceType;

    /** List cung: "Lần đầu" hoac "Phát hành lại" (khong FK, gioi han o UI). */
    @Column(name = "issuance_occurrence", length = 20)
    private String issuanceOccurrence;

    @Column(name = "sample_reason", length = 50)
    private String sampleReason;

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
