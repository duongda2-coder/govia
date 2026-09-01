package com.govia.audit.cmntd4.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Man hinh "Danh sach chon mau LC va nho thu TTQT" (sheet ZTC_CM_NTD4, phu luc 06B/BKS-KTNB) -
 * trong nhom "Thuc hien kiem toan" cua "Lap ke hoach", module Kiem toan noi bo. Cac cot kieu "List"
 * tham chieu du lieu IPCAS (ma chi nhanh/ma KH) chua co danh muc tuong ung trong GOVIA nen duoc luu
 * la String tu do, khong FK - cung quy uoc voi AuditBranchStaff.branchCode. */
@Getter
@Setter
@Entity
@Table(name = "audit_cm_ntd4")
public class AuditCmNtd4 extends BaseEntity {

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "reference_number", nullable = false, precision = 20, scale = 2)
    private BigDecimal referenceNumber;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "customer_code", length = 50)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "beneficiary", length = 20)
    private String beneficiary;

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
